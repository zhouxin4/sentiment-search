package zx.soft.sent.solr.origin;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.common.domain.QueryParams;
import zx.soft.sent.common.domain.SentimentConstant;
import zx.soft.sent.dao.domain.allinternet.InternetTask;
import zx.soft.sent.solr.query.QueryCore;
import zx.soft.sent.solr.utils.RecentNList;
import zx.soft.sent.solr.utils.RedisMQ;
import zx.soft.sent.solr.utils.StringUtils;
import zx.soft.utils.json.JsonUtils;
import zx.soft.utils.log.LogbackUtil;
import zx.soft.utils.time.TimeUtils;

import com.google.common.base.Strings;

/**
 * OA溯源任务查询线程
 *
 * @author donglei
 *
 */
public class OriginCacheRedisRunnable implements Runnable {

	private static Logger logger = LoggerFactory.getLogger(OriginCacheRedisRunnable.class);

	// 搜索查询类
	private QueryCore search;

	private RedisMQ redisMQ;

	// 一个任务信息
	private InternetTask task;

	// 全局计数器
	//	private static final AtomicInteger COUNT = new AtomicInteger(0);

	public OriginCacheRedisRunnable(QueryCore search, RedisMQ redisMQ, InternetTask task) {
		this.search = search;
		this.redisMQ = redisMQ;
		this.task = task;
	}

	@Override
	public void run() {
		try {
			/*
			 * 执行查询
			 */
			List<String> cacheDocs = new ArrayList<>();
			RecentNList<String> recentLists = new RecentNList<>(20);
			QueryParams queryParams = new QueryParams();
			queryParams.setQ(task.getKeywords());
			queryParams.setFq("first_time:[2000-01-01T00:00:00Z TO " + task.getEnd_time() + "]");
			queryParams.setStart(0);
			queryParams.setRows(100);
			QueryResponse first = search.queryDataWithoutView(queryParams, true);
			long size = first.getResults().getNumFound() > 500 ? 500 : first.getResults().getNumFound();
			cacheSolrResponseToRedis(first, recentLists);
			for (int i = 100; i < size; i += 100) {
				queryParams.setStart(i);
				QueryResponse more = search.queryDataWithoutView(queryParams, true);
				cacheSolrResponseToRedis(more, recentLists);

			}

		} catch (Exception e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
		}
	}

	private void cacheSolrResponseToRedis(QueryResponse result, RecentNList<String> recentLists) {
		long numFound = result.getResults().getNumFound();
		List<String> cacheDocs = new ArrayList<>();
		for (SolrDocument doc : result.getResults()) {
			if (doc.getFieldValue("content") == null) {
				continue;
			}
			String content = doc.getFieldValue("content").toString();
			if (Strings.isNullOrEmpty(content)) {
				continue;
			}
			if (content.length() > 500) {
				content = content.substring(0, 500);
			}
			boolean repeat = false;
			for (String str : recentLists.getLists()) {
				int dis = StringUtils.getLevenshteinDistance(str, content);
				if (dis * 1.0 / content.length() < 0.2) {
					repeat = true;
					break;
				}
			}
			if (!repeat) {
				doc.setField("cache_type", 1);
				doc.setField("cache_id", task.getIdentify());
				doc.setField("cache_value", (int) numFound);
				doc.setField("id", "1_" + doc.getFieldValue("id").toString());
				tackleTime(doc);
				cacheDocs.add(JsonUtils.toJsonWithoutPretty(doc));
				recentLists.addElement(content);
			}
		}
		redisMQ.addRecord(SentimentConstant.CDH5_CACHE_RECORDS, cacheDocs.toArray(new String[cacheDocs.size()]));
		logger.info("cache {} records to redis", cacheDocs.size());
	}

	/**
	 * 查询时间有8小时误差，在这里修正
	 */
	private void tackleTime(SolrDocument result) {
		if (result.getFieldValueMap().get("timestamp") != null) {
			result.setField("timestamp", TimeUtils.transCurrentTime(
					TimeUtils.transSolrReturnStrToMilli(result.getFieldValueMap().get("timestamp").toString()), 0, 0,
					0, -8));
		}
		if (result.getFieldValueMap().get("lasttime") != null) {
			result.setField("lasttime", TimeUtils.transCurrentTime(
					TimeUtils.transSolrReturnStrToMilli(result.getFieldValueMap().get("lasttime").toString()), 0, 0, 0,
					-8));
		}
		if (result.getFieldValueMap().get("first_time") != null) {
			result.setField("first_time", TimeUtils.transCurrentTime(
					TimeUtils.transSolrReturnStrToMilli(result.getFieldValueMap().get("first_time").toString()), 0, 0,
					0, -8));
		}
		if (result.getFieldValueMap().get("update_time") != null) {
			result.setField(
					"update_time",
					TimeUtils.transCurrentTime(TimeUtils.transSolrReturnStrToMilli(result.getFieldValueMap()
							.get("update_time").toString()), 0, 0, 0, -8));
		}
	}
}
