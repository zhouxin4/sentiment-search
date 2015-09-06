package zx.soft.sent.solr.origin;

import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.core.domain.QueryParams;
import zx.soft.sent.dao.domain.allinternet.InternetTask;
import zx.soft.sent.dao.insight.RiakInsight;
import zx.soft.sent.solr.domain.QueryResult;
import zx.soft.sent.solr.query.QueryCore;
import zx.soft.utils.json.JsonUtils;
import zx.soft.utils.log.LogbackUtil;
import zx.soft.utils.time.TimeUtils;

/**
 * OA溯源任务查询线程
 *
 * @author donglei
 *
 */
public class OriginUpdateRunnable implements Runnable {

	private static Logger logger = LoggerFactory.getLogger(OriginUpdateRunnable.class);

	// 搜索查询类
	private QueryCore search;
	// 持久化类
	private RiakInsight riakAccess;
	// 一个任务信息
	private InternetTask task;

	// 全局计数器
	//	private static final AtomicInteger COUNT = new AtomicInteger(0);

	public OriginUpdateRunnable(QueryCore search, RiakInsight riakAccess, InternetTask task) {
		this.search = search;
		this.riakAccess = riakAccess;
		this.task = task;
	}

	@Override
	public void run() {
		try {
			/*
			 * 执行查询
			 */
			QueryParams queryParams = new QueryParams();
			queryParams.setQ(task.getKeywords());
			queryParams.setFq("first_time:[2000-01-01T00:00:00Z TO " + task.getEnd_time() + "]");
			queryParams.setRows(100);
			QueryResult result = search.queryData(queryParams, true);
			int localCount = (int) result.getNumFound();
			OriginPostModel originPost = new OriginPostModel();
			originPost.setCount(localCount);
			originPost.setOrigin(localCount > 100 ? 100 : localCount);
			originPost.setUpdateTime(TimeUtils.transToSolrDateStr(System.currentTimeMillis()));
			int i = 0;
			for (SolrDocument doc : result.getResults()) {
				if (doc.getFieldValue("content") != null && doc.getFieldValue("content").toString().length() > 40000) {
					continue;
				}
				originPost.addDoc(doc);
				i++;
				if (i % 10 == 0) {
					originPost.setPage(i / 10);
					riakAccess.insertHotkeys("origins", task.getIdentify() + "_P" + (i / 10),
							JsonUtils.toJsonWithoutPretty(originPost));
					originPost.getDocs().clear();
				}
			}
			if (i % 10 != 0 || i == 0) {
				originPost.setPage(i / 10 + 1);
				riakAccess.insertHotkeys("origins", task.getIdentify() + "_P" + (i / 10 + 1),
						JsonUtils.toJsonWithoutPretty(originPost));
				originPost.getDocs().clear();
			}
		} catch (Exception e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
		}
	}
}
