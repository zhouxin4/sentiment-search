package zx.soft.sent.solr.insight;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.core.domain.QueryParams;
import zx.soft.sent.dao.insight.RiakInsight;
import zx.soft.sent.solr.domain.QueryResult;
import zx.soft.sent.solr.insight.Virtuals.Virtual;
import zx.soft.sent.solr.query.QueryCore;
import zx.soft.utils.algo.TopN;
import zx.soft.utils.algo.TopN.KeyValue;
import zx.soft.utils.http.HttpClientDaoImpl;
import zx.soft.utils.json.JsonNodeUtils;
import zx.soft.utils.json.JsonUtils;
import zx.soft.utils.log.LogbackUtil;
import zx.soft.utils.time.TimeUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.hankcs.hanlp.HanLP;

/**
 * 重点人员分时段统计热们关键词：hefei06
 *
 * 运行目录：/home/zxdfs/run-work/timer/insight
 * 运行命令：./insighthotkey.sh &
 * @author donglei
 *
 */
public class InsightHotKey {

	private final static int NUM_EACH_POST = 10;
	private static Logger logger = LoggerFactory.getLogger(InsightHotKey.class);

	public static final String VIRTUAL_URL = "http://192.168.32.20:8080/keyusers/virtualUser";
	public static final String TRUE_USER = "http://192.168.32.20:8080/keyusers/trueUser/";

	public InsightHotKey() {
		//
	}

	/**
	 * 主函数
	 */
	public static void main(String[] args) {
		InsightHotKey firstPageRun = new InsightHotKey();
		try {
			firstPageRun.run();
		} catch (Exception e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
		}
	}

	public void run() {
		logger.info("Starting generate data...");
		RiakInsight insight = new RiakInsight();
		long current = getHourTime(System.currentTimeMillis());
		current = TimeUtils.transCurrentTime(current, 0, 0, 0, 1);
		for (AreaCode area : AreaCode.values()) {
			String areaCode = area.getAreaCode();
			List<UserDomain> trueUsers = getTrueUser(areaCode);
			for (UserDomain user : trueUsers) {
				String trueUserId = user.getTureUserId();
				for (int i = 0; i < 720; i++) {
					long hours = TimeUtils.transCurrentTime(current, 0, 0, 0, -i);
					Multiset<String> counts = getOneDayHotKeys(trueUserId, hours);
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

	private Multiset<String> getOneDayHotKeys(String trueUserId, long milliTime) {
		Multiset<String> counts = HashMultiset.create();
		QueryParams params = new QueryParams();
		params.setQ("*:*");
		long current = milliTime;
		long last = TimeUtils.transCurrentTime(current, 0, 0, 0, -1);
		params.setFq("timestamp:[" + TimeUtils.transToSolrDateStr(last) + " TO "
				+ TimeUtils.transToSolrDateStr(current) + "]");
		List<Virtual> virtuals = getVirtuals(trueUserId);
		if (virtuals.isEmpty()) {
			return counts;
		}
		RawType postsResult = new RawType();
		for (Virtual virtual : virtuals) {
			postsResult.addQueryParam("fq",
					"(nickname:" + virtual.getNickname() + " AND source_id:" + virtual.getSource_id() + ")");
		}
		params.setFq(params.getFq() + ";" + postsResult.getQueryParams().get("fq"));
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
				List<String> hotKeys = HanLP.extractKeyword(content, NUM_EACH_POST);
				counts.addAll(hotKeys);
			}
		}
	}

	public List<Virtual> getVirtuals(String trueUser) {
		String data = "{\"trueUserId\":\"" + trueUser + "\",\"page\":1,\"size\":100}";
		HttpClientDaoImpl httpclient = new HttpClientDaoImpl();
		long start = System.currentTimeMillis();
		String response = httpclient.doPostAndGetResponse(VIRTUAL_URL, data);
		logger.info("获取虚拟账号耗时: {}", System.currentTimeMillis() - start);
		if (!"error".equals(response)) {
			JsonNode node = JsonNodeUtils.getJsonNode(response, "response");
			List<Virtual> virs = JsonUtils.parseJsonArray(node.toString(), Virtual.class);
			return virs;
		}
		return new ArrayList<>();
	}

	public List<UserDomain> getTrueUser(String areaCode) {
		String data = "{\"areaCode\":" + areaCode + "}";
		HttpClientDaoImpl httpclient = new HttpClientDaoImpl();
		String response = httpclient.doPostAndGetResponse(TRUE_USER, data);
		if (!"error".equals(response)) {
			JsonNode node = JsonNodeUtils.getJsonNode(response, "response");
			List<UserDomain> virs = JsonUtils.parseJsonArray(node.toString(), UserDomain.class);
			return virs;
		}
		return new ArrayList<>();
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
