package zx.soft.sent.solr.insight;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ansj.app.keyword.KeyWordComputer;
import org.ansj.app.keyword.Keyword;
import org.ansj.domain.Term;
import org.ansj.recognition.NatureRecognition;
import org.ansj.util.FilterModifWord;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.common.domain.QueryParams;
import zx.soft.sent.common.insight.AreaCode;
import zx.soft.sent.common.insight.TrueUserHelper;
import zx.soft.sent.common.insight.UserDomain;
import zx.soft.sent.common.insight.Virtuals.Virtual;
import zx.soft.sent.dao.insight.RiakInsight;
import zx.soft.sent.solr.demo.HotKeyDemo;
import zx.soft.sent.solr.domain.QueryResult;
import zx.soft.sent.solr.query.QueryCore;
import zx.soft.utils.algo.TopN;
import zx.soft.utils.algo.TopN.KeyValue;
import zx.soft.utils.json.JsonUtils;
import zx.soft.utils.log.LogbackUtil;
import zx.soft.utils.string.ConcatMethod;
import zx.soft.utils.string.StringConcatHelper;
import zx.soft.utils.time.TimeUtils;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**
 * 重点人员分时段统计热们关键词：hefei06
 *
 * 运行目录：/home/zxdfs/run-work/timer/insight
 * 运行命令：./insighthotkey.sh &
 * @author donglei
 *
 */
public class InsightHotKey {

	private static Logger logger = LoggerFactory.getLogger(InsightHotKey.class);

	private final static int NUM_EACH_POST = 40;

	private static KeyWordComputer kwc = new KeyWordComputer(NUM_EACH_POST);

	private RiakInsight insight = null;

	public InsightHotKey() {
		insight = new RiakInsight();
	}

	/**
	 * 主函数
	 */
	public static void main(String[] args) {
		InsightHotKey firstPageRun = new InsightHotKey();
		try (BufferedReader read = new BufferedReader(new InputStreamReader(HotKeyDemo.class.getClassLoader()
				.getResourceAsStream("stopwords_zh.txt"), Charset.forName("UTF-8")));) {
			String line = null;
			while ((line = read.readLine()) != null) {
				if (!line.isEmpty()) {
					FilterModifWord.insertStopWord(line.trim());
				}
			}
		} catch (IOException e) {
			logger.error(LogbackUtil.expection2Str(e));
		}

		FilterModifWord.insertStopNatures("m");
		FilterModifWord.insertStopNatures("r");
		FilterModifWord.insertStopNatures("o");
		FilterModifWord.insertStopNatures("d");

		try {
			//			firstPageRun.run();
			firstPageRun.insertTrueUserHotKey();
		} catch (Exception e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
		}
		System.exit(0);
	}

	public void run() {
		logger.info("Starting generate data...");
		long current = getHourTime(System.currentTimeMillis());
		current = TimeUtils.transCurrentTime(current, 0, 0, 0, 1);
		for (AreaCode area : AreaCode.values()) {
			String areaCode = area.getAreaCode();
			List<UserDomain> trueUsers = TrueUserHelper.getTrueUsers(areaCode);
			for (UserDomain user : trueUsers) {
				String trueUserId = user.getTureUserId();
				for (int i = 0; i < 24; i++) {
					long hours = TimeUtils.transCurrentTime(current, 0, 0, 0, -i);
					Multiset<String> counts = getOneHourHotKeys(trueUserId, hours);
					Map<String, Integer> hotKeys = getTopNHotKey(counts, 20);
					if (!hotKeys.isEmpty()) {
						insight.insertHotkeys("hotkeys", trueUserId + "_" + TimeUtils.timeStrByHour(hours),
								JsonUtils.toJsonWithoutPretty(hotKeys));
						logger.info(hotKeys.toString());
					}
				}
			}
		}
		// 关闭资源
		insight.close();
		QueryCore.getInstance().close();
		logger.info("Finishing query OA-FirstPage data...");
	}

	private void insertTrueUserHotKey() {
		long milliTime = System.currentTimeMillis();
		for (AreaCode area : AreaCode.values()) {
			String areaCode = area.getAreaCode();
			List<UserDomain> trueUsers = TrueUserHelper.getTrueUsers(areaCode);
			for (UserDomain user : trueUsers) {
				String trueUserId = user.getTureUserId();
				for (int i = 5 * 720; i < 9 * 720; i++) {
					long hours = TimeUtils.transCurrentTime(milliTime, 0, 0, 0, -i);
					Multiset<String> counts = getOneHourHotKeys(trueUserId, hours);
					Map<String, Integer> hotKeys = getTopNHotKey(counts, 20);
					if (!hotKeys.isEmpty()) {
						insight.insertHotkeys("hotkeys", trueUserId + "_" + TimeUtils.timeStrByHour(hours),
								JsonUtils.toJsonWithoutPretty(hotKeys));
						logger.info(hotKeys.toString());
					}
				}

			}
		}
	}

	private Multiset<String> getOneHourHotKeys(String trueUserId, long milliTime) {
		Multiset<String> counts = HashMultiset.create();
		QueryParams params = new QueryParams();
		params.setQ("*:*");
		long current = milliTime;
		long last = TimeUtils.transCurrentTime(current, 0, 0, 0, -1);
		params.setFq("timestamp:[" + TimeUtils.transToSolrDateStr(last) + " TO "
				+ TimeUtils.transToSolrDateStr(current) + "]");
		List<Virtual> virtuals = TrueUserHelper.getVirtuals(trueUserId);
		if (virtuals.isEmpty()) {
			return counts;
		}
		StringConcatHelper helper = new StringConcatHelper(ConcatMethod.OR);

		for (Virtual virtual : virtuals) {
			if (virtual.getNickname().contains("\\")) {
				helper.add("(nickname:" + virtual.getNickname().replaceAll("[\\\\]", "?") + " AND source_id:"
						+ virtual.getSource_id() + ")");
			} else {
				helper.add("(nickname:\"" + virtual.getNickname() + "\" AND source_id:" + virtual.getSource_id() + ")");
			}
		}
		params.setFq(params.getFq() + ";" + helper.getString());
		params.setRows(200);
		QueryResult result = QueryCore.getInstance().queryData(params, false);
		countHotKeys(result, counts);
		long numFound = result.getNumFound() > 10000 ? 10000 : result.getNumFound();
		for (int i = 200; i < numFound; i += 200) {
			params.setStart(i);
			params.setRows(200);
			result = QueryCore.getInstance().queryData(params, false);
			countHotKeys(result, counts);
		}
		return counts;
	}

	private Map<String, Integer> getTopNHotKey(Multiset<String> counts, int N) {
		Map<String, Integer> hotKeys = new HashMap<>();
		if (counts.isEmpty()) {
			return hotKeys;
		}
		List<KeyValue<String, Integer>> topN = TopN.topNOnValue(counts, N);
		for (KeyValue<String, Integer> keyValue : topN) {
			hotKeys.put(keyValue.getKey(), keyValue.getValue());
		}
		return hotKeys;
	}

	public void countHotKeys(QueryResult result, Multiset<String> counts) {
		for (SolrDocument doc : result.getResults()) {
			String content = (String) doc.getFieldValue("content");
			if (content != null) {
				content = content.replaceAll("[http|https]+[://]+[0-9A-Za-z:/[-]_#[?][=][.][&]]*", "");
				Collection<Keyword> keywords = kwc.computeArticleTfidf(content);
				List<String> words = new ArrayList<String>();
				for (Keyword keyword : keywords) {
					words.add(keyword.getName());
				}
				List<Term> recognition = NatureRecognition.recognition(words, 0);
				recognition = FilterModifWord.modifResult(recognition);
				for (Term term : recognition) {
					counts.add(term.getName());
				}
			}
		}
	}

	public static long getHourTime(long milli) {
		Calendar date = Calendar.getInstance();
		date.setTimeInMillis(milli);
		date.set(Calendar.MINUTE, 0);
		date.set(Calendar.SECOND, 0);
		date.set(Calendar.MILLISECOND, 0);
		return date.getTimeInMillis();
	}

}
