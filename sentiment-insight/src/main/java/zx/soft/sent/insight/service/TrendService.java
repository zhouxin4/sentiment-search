package zx.soft.sent.insight.service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import zx.soft.sent.common.domain.QueryParams;
import zx.soft.sent.common.insight.Group;
import zx.soft.sent.common.insight.TrueUserHelper;
import zx.soft.sent.common.insight.Virtuals.Virtual;
import zx.soft.sent.dao.insight.RiakInsight;
import zx.soft.sent.insight.domain.TrendResult;
import zx.soft.sent.insight.utils.QueryCallable;
import zx.soft.sent.solr.domain.QueryResult;
import zx.soft.utils.algo.TopN;
import zx.soft.utils.algo.TopN.KeyValue;
import zx.soft.utils.json.JsonUtils;
import zx.soft.utils.string.ConcatMethod;
import zx.soft.utils.string.StringConcatHelper;
import zx.soft.utils.threads.AwesomeThreadPool;
import zx.soft.utils.time.TimeUtils;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**
 * @author donglei
 */
@Service
public class TrendService {

	private static RiakInsight riak = null;
	static {
		riak = new RiakInsight();
	}

	private static Logger logger = LoggerFactory.getLogger(TrendService.class);

	public Object getTrendInfos(final QueryParams params, final String nickname, final boolean isUser) {
		long start = System.currentTimeMillis();
		// 获得热门关键词
		Callable<TrendResult> hotkeyCall = new Callable<TrendResult>() {

			@Override
			public TrendResult call() throws Exception {
				TrendResult hotkeyResult = new TrendResult();
				List<KeyValue<String, Integer>> hotKeys = getHotKeys(params, nickname);
				for (KeyValue<String, Integer> hotKey : hotKeys) {
					hotkeyResult.countHotWords(hotKey.getKey(), hotKey.getValue());
				}
				hotkeyResult.sortHotKeys();
				return hotkeyResult;
			}
		};
		FutureTask<TrendResult> hotkeyTask = new FutureTask<>(hotkeyCall);
		new Thread(hotkeyTask).start();
		Callable<TrendResult> trendCall = new Callable<TrendResult>() {

			@Override
			public TrendResult call() throws Exception {
				TrendResult trendResult = new TrendResult();
				List<Virtual> virtuals = TrueUserHelper.getVirtuals(nickname);
				//				if (virtuals.isEmpty()) {
				//					logger.info("True user '{}': has no virtuals!", nickname);
				//					return trendResult;
				//				}
				List<String> fqs = new ArrayList<>();
				int i = 0;
				StringConcatHelper helper = new StringConcatHelper(ConcatMethod.OR);
				for (Virtual virtual : virtuals) {
					helper.add("(nickname:\"" + virtual.getNickname() + "\" AND source_id:" + virtual.getSource_id()
							+ ")");
					i++;
					if (i == 8) {
						fqs.add(helper.getString());
						i = 0;
						helper.clear();
					}
				}
				if (i != 0) {
					fqs.add(helper.getString());
				}

				List<Callable<QueryResult>> calls = new ArrayList<>();
				List<Group> groups = TrueUserHelper.getTrendGroup(isUser);
				for (Group group : groups) {
					final String cate = group.getValue();
					trendResult.countTrend(cate, 0);
					helper.clear();
					for (String word : group.getKeywords().split(",")) {
						helper.add("\"" + word + "\"");
					}
					for (String fq : fqs) {
						final QueryParams tmp = params.clone();
						tmp.setFq(tmp.getFq() + ";" + fq);
						tmp.setQ(helper.getString());
						tmp.setRows(0);
						calls.add(new QueryCallable(cate, tmp));
					}
				}
				if (calls.isEmpty()) {
					logger.info("True user '{}': has no groups or keywords!", nickname);
					return trendResult;
				}
				int numThread = calls.size() > 10 ? 10 : calls.size();
				List<QueryResult> queryResults = AwesomeThreadPool.runCallables(numThread, calls);

				for (QueryResult queryResult : queryResults) {
					trendResult.countTrend(queryResult.getTag(), queryResult.getNumFound());
				}

				return trendResult;
			}
		};
		FutureTask<TrendResult> trendTask = new FutureTask<>(trendCall);
		new Thread(trendTask).start();
		TrendResult hotkey = new TrendResult();
		TrendResult trend = new TrendResult();
		try {
			hotkey = hotkeyTask.get(5, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.error(e.getMessage());
		}
		try {
			trend = trendTask.get(5, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.error(e.getMessage());
		}
		List<KeyValue<String, Long>> topNTrends = TopN.topNOnValue(trend.getTrends(), 6);
		for (KeyValue<String, Long> keyValue : topNTrends) {
			hotkey.countTrend(keyValue.getKey(), keyValue.getValue());
		}
		logger.info("获得倾向信息耗时: {}ms", System.currentTimeMillis() - start);

		return hotkey;
	}

	private List<KeyValue<String, Integer>> getHotKeys(QueryParams params, final String nickname) {
		Multiset<String> hotKeys = HashMultiset.create();
		long endTime = TimeUtils.getZeroHourTime(System.currentTimeMillis());
		long startTime = TimeUtils.transCurrentTime(endTime, 0, -1, 0, 0);
		if (params.getFq().contains("timestamp")) {
			for (String fq : params.getFq().split(";")) {
				if (fq.contains("timestamp")) {
					int li = fq.indexOf("[");
					int ri = fq.indexOf("TO");
					int lf = fq.indexOf("]");
					try {
						long lTime = TimeUtils.tranSolrDateStrToMilli(fq.substring(li + 1, ri).trim());
						startTime = TimeUtils.getZeroHourTime(lTime);
						long rTime = TimeUtils.tranSolrDateStrToMilli(fq.substring(ri + 2, lf).trim());
						endTime = TimeUtils.getZeroHourTime(rTime);
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
				break;
			}
		}
		List<Callable<Map<String, Integer>>> calls = new ArrayList<Callable<Map<String, Integer>>>();
		while (startTime < endTime) {
			calls.add(new RiakCallable(nickname, startTime));
			startTime = TimeUtils.transCurrentTime(startTime, 0, 0, 0, 1);
		}
		List<Map<String, Integer>> hoursHotKeys = AwesomeThreadPool.runCallables(5, calls);
		for (Map<String, Integer> hourHotKey : hoursHotKeys) {
			for (Entry<String, Integer> entry : hourHotKey.entrySet()) {
				hotKeys.add(entry.getKey(), entry.getValue());
			}
		}

		return TopN.topNOnValue(hotKeys, 20);
	}

	public static class RiakCallable implements Callable<Map<String, Integer>> {
		private String nickname;
		private long milliSecond;

		public RiakCallable(String nickname, long milliSecond) {
			this.nickname = nickname;
			this.milliSecond = milliSecond;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Map<String, Integer> call() throws Exception {
			String hourhotkeys = riak.selectHotkeys("hotkeys", nickname + "_" + TimeUtils.timeStrByHour(milliSecond));
			if (hourhotkeys != null) {
				Map<String, Integer> hotKeys = JsonUtils.getObject(hourhotkeys, Map.class);
				if (hotKeys != null) {
					return hotKeys;
				}
			}
			return new HashMap<>();
		}

	}
}
