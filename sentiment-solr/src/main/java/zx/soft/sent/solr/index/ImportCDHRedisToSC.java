package zx.soft.sent.solr.index;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.dao.common.SentimentConstant;
import zx.soft.sent.solr.query.QueryCore;
import zx.soft.sent.solr.utils.RedisMQ;
import zx.soft.utils.json.JsonUtils;

/**
 * 将Redis消息队列中的数据所引到SolrCloud：hefei09,hefei10运行
 *
 * 运行目录：/home/zxdfs/run-work/index
 * 运行命令： cd sentiment-solr
 *         bin/ctl.sh start importRedisToSC
 *
 * @author wanggang
 *
 */
public class ImportCDHRedisToSC {

	private static Logger logger = LoggerFactory.getLogger(ImportCDHRedisToSC.class);

	private final QueryCore core;

	private final RedisMQ redisMq;

	public ImportCDHRedisToSC() {
		core = QueryCore.getInstance();
		redisMq = new RedisMQ();
	}

	/**
	 * 主函数
	 */
	public static void main(String[] args) {
		ImportCDHRedisToSC importRedisToSC = new ImportCDHRedisToSC();
		importRedisToSC.index();
	}

	public void index() {
		List<String> records;
		List<SolrInputDocument> result = new ArrayList<>();
		String firstTime = null;
		while (true) {
			// 批量Add并Commit
			logger.info("Starting index ...");
			records = redisMq.getRecords(SentimentConstant.CDH5_CACHE_RECORDS);
			for (String record : records) {
				SolrDocument doc = JsonUtils.getObject(record, SolrDocument.class);
				result.add(QueryCore.transSolrDocumentToInputDocument(doc));
			}
			core.addDocToSolr(result);
			try {
				Thread.sleep(1_000);
			} catch (InterruptedException e) {
				// TODO
			}
			logger.info("Finishing index ...");
		}
	}

	public void close() {
		core.close();
		redisMq.close();
	}

}
