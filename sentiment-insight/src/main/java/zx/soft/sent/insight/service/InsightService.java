package zx.soft.sent.insight.service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.client.solrj.response.RangeFacet.Count;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import zx.soft.sent.core.domain.QueryParams;
import zx.soft.sent.dao.insight.RiakInsight;
import zx.soft.sent.insight.domain.CommonRequest;
import zx.soft.sent.insight.domain.CommonRequest.UserInfo;
import zx.soft.sent.insight.domain.Group;
import zx.soft.sent.insight.domain.Group.KeyWord;
import zx.soft.sent.insight.domain.PostsResult;
import zx.soft.sent.insight.domain.TrendResult;
import zx.soft.sent.insight.utils.VirtualsCache;
import zx.soft.sent.solr.domain.QueryResult;
import zx.soft.sent.solr.domain.SimpleFacetInfo;
import zx.soft.sent.solr.insight.RawType;
import zx.soft.sent.solr.insight.UserDomain;
import zx.soft.sent.solr.insight.Virtuals;
import zx.soft.sent.solr.insight.Virtuals.Virtual;
import zx.soft.sent.solr.query.QueryCore;
import zx.soft.sent.solr.query.QueryCore.Shards;
import zx.soft.utils.algo.TopN;
import zx.soft.utils.algo.TopN.KeyValue;
import zx.soft.utils.http.HttpClientDaoImpl;
import zx.soft.utils.json.JsonNodeUtils;
import zx.soft.utils.json.JsonUtils;
import zx.soft.utils.string.ConcatMethod;
import zx.soft.utils.string.StringConcatHelper;
import zx.soft.utils.threads.AwesomeThreadPool;
import zx.soft.utils.time.TimeUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.primitives.Longs;

/**
 * @author donglei
 */
@Service
public class InsightService {

	private static Logger logger = LoggerFactory.getLogger(InsightService.class);
	public static final String VIRTUAL_URL = "http://192.168.32.20:8080/keyusers/virtualUser";
	public static final String GROUP_URL = "http://192.168.32.20:8080/keyusers/keyword";
	public static final String TRUE_USER = "http://192.168.32.20:8080/keyusers/trueUser/";

	private static QueryCore core = null;
	private static RiakInsight riak = null;
	static {
		core = new QueryCore();
		riak = new RiakInsight();
	}

	private static Map<String, String> cates = new HashMap<String, String>();
	static {
		cates.put("民主思潮", "专制独裁民主民主制选举立法政协党派多党合作上访贪官");
		cates.put("食品安全", "食品路边小吃死猪肉麻辣烫地沟油福尔马林细菌寄生虫硫酸亚铁病菌僵尸肉");
		cates.put("敌对势力", "敌对势力藏独达赖分裂势力台独恐怖分子");
		cates.put("利益垄断", "利益财富资源资金 AND 垄断把持操控独占独揽");
		cates.put("涉及消防", "消防燃气液化气酒精高温家用电器  AND  火灾爆炸灭火器失火泄漏");
		cates.put("涉及民生", "民生医药费食品安全公共设施");
		cates.put("周边国家", "韩国日本菲律宾马来西亚文莱印度尼西亚新加坡越南朝鲜俄罗斯蒙古哈萨克斯坦吉尔吉斯塔吉克斯坦阿富汗巴基斯坦尼泊尔不丹缅甸老挝");
	}

	private Map<String, Long> getRelatedNickname(QueryParams params, String nickname) {
		long start = System.currentTimeMillis();
		List<Virtual> virtuals = getVirtuals(nickname);
		RawType util = new RawType();
		for (Virtual virtual : virtuals) {
			util.addQueryParam("fq", "\"" + virtual.getNickname() + "\"");
		}
		params.setFq(params.getFq() + ";" + "content:(" + util.getQueryParams().get("fq") + ")");
		params.setFacetField("nickname");
		params.setShard(true);
		List<Callable<QueryResult>> calls = new ArrayList<>();
		for (Shards shard : Shards.values()) {
			final QueryParams tmp = params.clone();
			tmp.setShardName(shard.name());
			calls.add(new MyCallable(shard.name(), tmp));
		}
		List<QueryResult> results = AwesomeThreadPool.runCallables(6, calls);

		for (QueryResult result : results) {
			for (SimpleFacetInfo facetInfo : result.getFacetFields()) {
				if ("nickname".equals(facetInfo.getName())) {
					for (Entry<String, Long> count : facetInfo.getValues().entrySet()) {
						util.countData(count.getKey(), count.getValue());
					}
				}
			}
		}
		for (Virtual vir : virtuals) {
			util.removeData(vir.getNickname());
		}
		logger.info("关系分析耗时: {}", System.currentTimeMillis() - start);
		return util.getDatas();
	}

	public Object relationAnalysed(QueryParams params, String nickname) {
		QueryParams tmp = params.clone();
		//		List<Virtual> virtuals = getVirtuals(nickname);
		//		RawType util = new RawType();
		Map<String, Long> count = getRelatedNickname(tmp, nickname);
		//      暂时只考虑其他账号和真实用户的关系
		//		for (Virtual virtual : virtuals) {
		//			util.addQueryParam("fq", "(nickname:" + virtual.getNickname() + " AND source_id:" + virtual.getSource_id()
		//					+ ")");
		//		}
		//		params.setFq(params.getFq() + ";" + util.getQueryParams().get("fq"));
		//		List<Callable<QueryResult>> calls = new ArrayList<>();
		//		for (String rel : count.keySet()) {
		//			QueryParams queryTmp = params.clone();
		//			queryTmp.setQ(queryTmp.getQ() + " AND " + "\"" + rel + "\"");
		//			calls.add(new MyCallable(rel, queryTmp));
		//		}
		//		List<QueryResult> queryResults = AwesomeThreadPool.runCallables(5, calls, QueryResult.class);
		//		for (QueryResult queryResult : queryResults) {
		//			if (count.containsKey(queryResult.getTag())) {
		//				count.put(queryResult.getTag(), queryResult.getNumFound() + count.get(queryResult.getTag()));
		//			} else {
		//				count.put(queryResult.getTag(), queryResult.getNumFound());
		//			}
		//		}
		List<Entry<String, Long>> hotRels = new ArrayList<>();
		for (Entry<String, Long> entry : count.entrySet()) {
			hotRels.add(entry);
		}
		Collections.sort(hotRels, new Comparator<Entry<String, Long>>() {

			@Override
			public int compare(Entry<String, Long> o1, Entry<String, Long> o2) {
				return -Longs.compare(o1.getValue(), o2.getValue());
			}
		});
		if (hotRels.size() < 10) {
			return hotRels;
		}

		return hotRels.subList(0, 10);
	}

	public QueryResult queryData(QueryParams params, String nickname) {
		List<Virtual> virtuals = getVirtuals(nickname);
		if (virtuals.isEmpty()) {
			return new QueryResult();
		}
		RawType fq = new RawType();
		for (Virtual virtual : virtuals) {
			fq.addQueryParam("fq", "(nickname:" + virtual.getNickname() + " AND source_id:" + virtual.getSource_id()
					+ ")");
		}
		params.setFq(params.getFq() + ";" + fq.getQueryParams().get("fq"));
		return core.queryData(params, false);
	}

	public Object getRelatedData(QueryParams params, String nickname) {
		List<Virtual> virtuals = getVirtuals(nickname);
		if (virtuals.isEmpty()) {
			logger.info("True user '{}': has no virtuals!", nickname);
			return new QueryResult();
		}
		StringConcatHelper helper = new StringConcatHelper(ConcatMethod.OR);
		for (Virtual virtual : virtuals) {
			helper.add("\"@" + virtual.getNickname() + "\"");
		}
		UserDomain trueUserInfo = getUserInfo(nickname);
		if(trueUserInfo == null) {
			logger.info("True user '{}' does not exist!", nickname);
			return new QueryResult();
		}
		helper.add("\"" + trueUserInfo.getUserName() + "\"");
		params.setFq(params.getFq() + ";content:(" + helper.getString() + ")");
		return core.queryData(params, false);
	}

	public Object getTrendInfos(final QueryParams params, final String nickname) {
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
				List<Virtual> virtuals = getVirtuals(nickname);
				if (virtuals.isEmpty()) {
					logger.info("True user '{}': has no virtuals!", nickname);
					return trendResult;
				}
				List<String> fqs = new ArrayList<>();
				int i = 0;
				StringConcatHelper helper = new StringConcatHelper(ConcatMethod.OR);
				for (Virtual virtual : virtuals) {
					helper.add("(nickname:" + virtual.getNickname() + " AND source_id:" + virtual.getSource_id() + ")");
					i++;
					if (i == 8) {
						fqs.add(helper.getString());
						i = 0;
						helper.clear();
					}
				}

				List<Callable<QueryResult>> calls = new ArrayList<>();
				List<Group> groups = getTrendGroup(nickname);
				for (Group group : groups) {
					final String cate = group.getUnit().getValue();
					trendResult.countTrend(cate, 0);
					helper.clear();
					for (KeyWord word : group.getKeyWords()) {
						helper.add("\"" + word.getValue() + "\"");
					}
					for (String fq : fqs) {
						final QueryParams tmp = params.clone();
						tmp.setFq(tmp.getFq() + ";" + fq);
						tmp.setQ(helper.getString());
						tmp.setRows(0);
						calls.add(new MyCallable(cate, tmp));
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
				List<KeyValue<String, Long>> keyValues = TopN.topNOnValue(trendResult.getTrends(), 6);
				trendResult.getTrends().clear();
				for (KeyValue<String, Long> keyValue : keyValues) {
					trendResult.countTrend(keyValue.getKey(), keyValue.getValue());
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
			trend = trendTask.get();
		} catch (InterruptedException | ExecutionException e) {
			logger.error(e.getMessage());
		}

		hotkey.setTrends(trend.getTrends());
		logger.info("获得倾向信息耗时: {}ms", System.currentTimeMillis() - start);

		return hotkey;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object getNicknamePostInfos(QueryParams params, String nickname) {
		long start = System.currentTimeMillis();
		PostsResult postsResults = new PostsResult();
		// 初始化postResult
		String fq = params.getFq();
		int source_id = -1;
		if (fq.contains("source_id")) {
			for (String sp : fq.split(";")) {
				if (sp.contains("source_id")) {
					source_id = Integer.parseInt(sp.split(":")[1]);
					postsResults.addDistIterm(source_id + "", 0);
				}
			}
		}
		try {
			long startTime = TimeUtils.tranSolrDateStrToMilli(params.getFacetRangeStart());
			long endTime = TimeUtils.tranSolrDateStrToMilli(params.getFacetRangeEnd());
			while (startTime < endTime) {
				String[] tmp = TimeUtils.timeStrByHour(startTime).split(",");
				String date = tmp[0].substring(tmp[0].indexOf("-") + 1);
				postsResults.addDateIterm(date, 0);
				postsResults.addHourIterm(tmp[1], 0);
				startTime = TimeUtils.transCurrentTime(startTime, 0, 0, 0, 1);
			}
		} catch (ParseException e) {
			logger.info(e.getMessage());
		}
		List<Virtual> virtuals = getVirtuals(nickname);
		if (virtuals.isEmpty()) {
			logger.info("True user '{}': has no virtuals!", nickname);
			return postsResults;
		}
		List<Callable<QueryResult>> calls = new ArrayList<>();

		for (final Virtual virtual : virtuals) {
			if (source_id != -1 && virtual.getSource_id() != source_id) {
				continue;
			}
			final QueryParams tmp = params.clone();
			if (tmp == null) {
				logger.error("Error throwed when Object cloned");
				continue;
			}
			tmp.setFq(tmp.getFq() + ";nickname:" + virtual.getNickname() + ";source_id:" + virtual.getSource_id());
			calls.add(new MyCallable(virtual.getSource_id() + "", tmp));
		}
		if (calls.isEmpty()) {
			logger.info("True user '{}': has no virtuals on Source_id {}!", nickname, source_id);
			return postsResults;
		}
		int numThread = calls.size() < 10 ? calls.size() : 10;
		//		int numThread = calls.size();
		List<QueryResult> queryResults = AwesomeThreadPool.runCallables(numThread, calls);
		for (QueryResult result : queryResults) {
			for (RangeFacet facet : result.getFacetRanges()) {
				if (facet.getName().equals(params.getFacetRange())) {
					List<Count> counts = facet.getCounts();
					for (Count count : counts) {
						postsResults.addIterm(result.getTag(), count.getValue(), count.getCount());
					}
				}
			}
		}
		logger.info("统计发帖情况耗时: {}ms", System.currentTimeMillis() - start);
		return postsResults;
	}

	public List<Virtual> getVirtuals(String trueUser) {
		Virtuals virtuals = VirtualsCache.getVirtuals(trueUser);
		if (virtuals != null && !virtuals.getVirtuals().isEmpty()) {
			return virtuals.getVirtuals();
		}
		String data = "{\"trueUserId\":\"" + trueUser + "\",\"page\":1,\"size\":100}";
		HttpClientDaoImpl httpclient = new HttpClientDaoImpl();
		long start = System.currentTimeMillis();
		String response = httpclient.doPostAndGetResponse(VIRTUAL_URL, data);
		logger.info("获取虚拟账号耗时: {}", System.currentTimeMillis() - start);
		if (!"error".equals(response)) {
			JsonNode node = JsonNodeUtils.getJsonNode(response, "response");
			List<Virtual> virs = JsonUtils.parseJsonArray(node.toString(), Virtual.class);
			if (!virs.isEmpty()) {
				VirtualsCache.addVirtuals(trueUser, virs);
			}
			return virs;
		}
		return new ArrayList<>();
	}

	public List<Group> getTrendGroup(String trueUser) {
		CommonRequest request = new CommonRequest();
		request.setType("unitword");
		request.setOperation(3);
		UserInfo unit = new UserInfo();
		unit.setCreator("201");
		unit.setCreatorAreaCode(340000);
		unit.setTrueUserId(trueUser);
		unit.setPage(1);
		unit.setSize(20);
		request.setInfo(unit);
		HttpClientDaoImpl httpclient = new HttpClientDaoImpl();
		long start = System.currentTimeMillis();
		String response = httpclient.doPostAndGetResponse(GROUP_URL, JsonUtils.toJsonWithoutPretty(request));
		logger.info("获取关键词分组耗时: {}", System.currentTimeMillis() - start);
		if (!"error".equals(response)) {
			JsonNode node = JsonNodeUtils.getJsonNode(response, "response");
			List<Group> group = JsonUtils.parseJsonArray(node.toString(), Group.class);
			return group;
		}
		return null;
	}

	public UserDomain getUserInfo(String trueUserId) {
		Virtuals virtuals = VirtualsCache.getVirtuals(trueUserId);
		if (virtuals != null && virtuals.getTrueUser() != null) {
			return virtuals.getTrueUser();
		}
		HttpClientDaoImpl httpClient = new HttpClientDaoImpl();
		String response = httpClient.doGet(TRUE_USER + trueUserId);
		JsonNode node = JsonNodeUtils.getJsonNode(response, "response");
		List<UserDomain> users = JsonUtils.parseJsonArray(node.toString(), UserDomain.class);
		if (users.isEmpty()) {
			return null;
		} else {
			VirtualsCache.addTrueUser(trueUserId, users.get(0));
			return users.get(0);
		}
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

	public static class MyCallable implements Callable<QueryResult> {
		private String tag = null;
		private QueryParams params = null;

		public MyCallable(String tag, QueryParams params) {
			this.tag = tag;
			this.params = params;
		}

		@Override
		public QueryResult call() throws Exception {
			QueryResult result = core.queryData(params, false);
			result.setTag(tag);
			return result;
		}

	}

	public static class RiakCallable implements Callable<Map<String, Integer>> {
		private String nickname;
		private long milliSecond;

		public RiakCallable(String nickname, long milliSecond) {
			this.nickname = nickname;
			this.milliSecond = milliSecond;
		}

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