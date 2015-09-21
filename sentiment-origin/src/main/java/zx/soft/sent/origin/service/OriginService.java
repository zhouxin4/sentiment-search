package zx.soft.sent.origin.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.client.solrj.response.RangeFacet.Count;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import zx.soft.sent.common.domain.QueryParams;
import zx.soft.sent.origin.utils.QueryCallable;
import zx.soft.sent.origin.utils.QueryCore;
import zx.soft.sent.origin.utils.QueryResult;
import zx.soft.utils.threads.AwesomeThreadPool;
import zx.soft.utils.time.TimeUtils;

/**
 * 索引服务类
 *
 * @author donglei
 *
 */
@Service
public class OriginService {

	private static Logger logger = LoggerFactory.getLogger(OriginService.class);

	private static final long ONEHOUR = 3600 * 1000;
	private static final long ONEDAY = 24 * ONEHOUR;
	private static final long ONEWEEK = 7 * ONEDAY;
	private static final long ONEMONTH = 4 * ONEWEEK;

	private static QueryCore core = QueryCore.getInstance();

	public Object getCounts(List<String> mds) {
		Map<String, Map<String, Integer>> results = new HashMap<String, Map<String, Integer>>();
		QueryParams params = new QueryParams();
		params.setQ("*:*");
		params.setFq("cache_type:1");
		params.setRows(1);
		List<Callable<QueryResult>> calls = new ArrayList<>();
		for (String md : mds) {
			final QueryParams tmp = params.clone();
			tmp.setFq("cache_id:" + md);
			calls.add(new QueryCallable(md, tmp));
		}
		if (calls.isEmpty()) {
			logger.info("There is  no mds!");
			return new HashMap<String, Long>();
		}
		List<QueryResult> queryResults = AwesomeThreadPool.runCallables(calls.size(), calls);
		for (QueryResult queryResult : queryResults) {
			Map<String, Integer> counts = new HashMap<>();
			if (queryResult.getNumFound() > 0) {
				counts.put("count",
						Integer.parseInt(queryResult.getResults().get(0).getFieldValue("cache_value").toString()));
				counts.put("origin", (int) queryResult.getNumFound());
			} else {
				counts.put("count", 0);
				counts.put("origin", 0);
			}
			results.put(queryResult.getTag(), counts);
		}

		return results;

	}

	public Object getOriginPosts(QueryParams params) {
		QueryParams queryParams = params.clone();
		QueryResult result = core.queryData(queryParams, true);
		for (SolrDocument doc : result.getResults()) {
			String id = doc.getFieldValue("id").toString();
			id = id.substring(id.indexOf("_") + 1, id.length());
			doc.setField("id", id);
		}
		return result;
	}

	public Object getOriginTrends(String key) {
		Map<String, Integer> maps = new TreeMap<>();
		QueryParams tmp = new QueryParams();
		tmp.setQ("*:*");
		tmp.setFq("cache_type:1;cache_id:" + key);
		tmp.setRows(1);
		tmp.setSort("timestamp:desc");
		QueryResult descResult = core.queryData(tmp, false);
		if (descResult.getNumFound() == 0) {
			return maps;
		}
		long maxTime = TimeUtils.transTimeLong(descResult.getResults().get(0).getFieldValue("timestamp").toString());
		tmp.setSort("timestamp:asc");
		QueryResult ascResult = core.queryData(tmp, false);
		if (ascResult.getNumFound() == 0) {
			return maps;
		}
		long minTime = TimeUtils.transTimeLong(ascResult.getResults().get(0).getFieldValue("timestamp").toString());
		long lastYear = TimeUtils.transCurrentTime(maxTime, -1, 0, 0, 0);
		minTime = minTime < lastYear ? lastYear : minTime;
		String gap = null;
		int type = 0;

		if ((maxTime - minTime) / 10 < ONEDAY) {
			gap = "+1DAY";
			type = 1;
		} else if ((maxTime - minTime) / 10 < ONEMONTH) {
			gap = "+" + (maxTime - minTime) / 10 / ONEDAY + "DAYS";
			type = 1;
		} else {
			gap = "+1MONTH";
			type = 2;
		}
		QueryParams queryParams = new QueryParams();
		queryParams.setQ("*:*");
		queryParams.setFacetRange("timestamp");
		queryParams.setFacetRangeStart(TimeUtils.transToSolrDateStr(minTime));
		queryParams.setFacetRangeEnd(TimeUtils.transToSolrDateStr(maxTime));
		queryParams.setFacetRangeGap(gap);
		queryParams.setFq(queryParams.getFq() + ";cache_type:1;cache_id:" + key);
		logger.info(queryParams.toString());
		QueryResult results = core.queryData(queryParams, false);
		for (RangeFacet facet : results.getFacetRanges()) {
			if (facet.getName().equals(queryParams.getFacetRange())) {
				List<Count> counts = facet.getCounts();
				for (Count count : counts) {
					String timeLine = count.getValue();
					if (type == 1) {
						timeLine = timeLine.substring(0, timeLine.indexOf("T"));
					} else if (type == 2) {
						timeLine = timeLine.substring(0, timeLine.lastIndexOf("-"));
					}
					maps.put(timeLine, count.getCount());
				}
			}
		}
		return maps;

	}
}