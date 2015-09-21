package zx.soft.sent.insight.service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.client.solrj.response.RangeFacet.Count;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import zx.soft.sent.common.domain.QueryParams;
import zx.soft.sent.common.insight.TrueUserHelper;
import zx.soft.sent.common.insight.Virtuals.Virtual;
import zx.soft.sent.insight.domain.PostsResult;
import zx.soft.sent.insight.utils.QueryCallable;
import zx.soft.sent.solr.domain.QueryResult;
import zx.soft.utils.threads.AwesomeThreadPool;
import zx.soft.utils.time.TimeUtils;

import com.google.common.collect.Collections2;

/**
 * 发帖情况统计模块
 * @author donglei
 */
@Service
public class PostService {
	private static Logger logger = LoggerFactory.getLogger(PostService.class);

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object getNicknamePostInfos(QueryParams params, String nickname) {
		long start = System.currentTimeMillis();
		PostsResult postsResults = new PostsResult();
		// 初始化postResult
		String fq = params.getFq();
		if (fq.contains("source_id")) {
			for (String sp : fq.split(";")) {
				if (sp.contains("source_id")) {
					postsResults.addDistIterm(sp.split(":")[1], 0);
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
		Collection<Virtual> virtuals = TrueUserHelper.getVirtuals(nickname);
		if (virtuals.isEmpty()) {
			logger.info("True user '{}': has no virtuals!", nickname);
			return postsResults;
		}
		List<Callable<QueryResult>> calls = new ArrayList<>();
		virtuals = Collections2.filter(virtuals, new TrueUserHelper.VirtualPredicate(fq));
		for (final Virtual virtual : virtuals) {
			final QueryParams tmp = params.clone();
			if (tmp == null) {
				logger.error("Error throwed when Object cloned");
				continue;
			}
			tmp.setFq(tmp.getFq() + ";nickname:\"" + virtual.getNickname() + "\";source_id:" + virtual.getSource_id());
			calls.add(new QueryCallable(virtual.getSource_id() + "", tmp));
		}
		if (calls.isEmpty()) {
			logger.info("True user '{}': has no virtuals on Source_id {}!", nickname, params.getFq());
			return postsResults;
		}
		int numThread = calls.size() < 10 ? calls.size() : 10;
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

}
