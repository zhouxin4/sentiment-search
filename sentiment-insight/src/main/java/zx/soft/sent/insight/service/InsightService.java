package zx.soft.sent.insight.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.client.solrj.response.RangeFacet.Count;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import zx.soft.sent.insight.domain.PostsResult;
import zx.soft.sent.solr.domain.QueryParams;
import zx.soft.sent.solr.domain.QueryResult;
import zx.soft.sent.solr.query.QueryCore;

/**
 * @author donglei
 */
@Service
public class InsightService {

	private static Logger logger = LoggerFactory.getLogger(InsightService.class);
	private static QueryCore core = null;
	static {
		core = new QueryCore();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object getNicknamePostInfos(QueryParams params, String nickname) {
		String virtuals[] = { "插排不够用呀", "每日新闻热点", "正源定慧福里北京" };
		ExecutorService executor = Executors.newFixedThreadPool(virtuals.length);
		List<QueryResult> queryResults = new ArrayList<QueryResult>();
		List<Future<QueryResult>> results = new ArrayList<Future<QueryResult>>();
		String fq = params.getFq();
		for (final String virtual : virtuals) {
			final QueryParams tmp = params.clone();
			if (tmp == null) {
				logger.error("Error throwed when Object cloned");
				continue;
			}
			tmp.setFq(fq + ";nickname:" + virtual);
			Future<QueryResult> result = executor.submit(new Callable<QueryResult>() {
				@Override
				public QueryResult call() throws Exception {
					QueryResult result = core.queryData(tmp, false);
					result.setTag(virtual);
					return result;
				}
			});

			results.add(result);
		}
		for (Future<QueryResult> result : results) {
			try {

				queryResults.add(result.get(20, TimeUnit.SECONDS));
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		executor.shutdown();
		PostsResult postsResults = new PostsResult();
		for (QueryResult result : queryResults) {
			for(RangeFacet facet : result.getFacetRanges()){
				if (facet.getName().equals(params.getFacetRange())) {
					List<Count> counts = facet.getCounts();
					for (Count count : counts) {
						//						System.out.println(result.getTag() + " " + count.getValue() + " " + count.getCount());
						postsResults.addIterm(result.getTag(), count.getValue(), count.getCount());
					}
				}
			}
		}
		return postsResults;
	}

}