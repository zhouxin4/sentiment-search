package zx.soft.sent.incompa.timer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.incompa.utils.QueryCore;
import zx.soft.sent.incompa.utils.RedisMQ;
import zx.soft.utils.json.JsonUtils;
import zx.soft.utils.log.LogbackUtil;

/**
 * 将Redis消息队列cdh5.cache.records中的数据所引到CDH5 solr集群中
 *
 * 运行目录： /root/run-work/timer/redis2cdhsolr
 * 运行命令： cd sina-IO
 * bin/ctl.sh start cdhRedisTOSC &
 * @author wanggang
 *
 */
public class ImportCDHRedisToSC {

	private static Logger logger = LoggerFactory.getLogger(ImportCDHRedisToSC.class);

	private final QueryCore core;

	private final RedisMQ redisMq;

	public ImportCDHRedisToSC() {
		try {
			core = new QueryCore();
		} catch (MalformedURLException e) {
			logger.error(LogbackUtil.expection2Str(e));
			throw new RuntimeException();
		}
		redisMq = new RedisMQ();
	}

	/**
	 * 主函数
	 */
//	public static void main(String[] args) {
//		ImportCDHRedisToSC importRedisToSC = new ImportCDHRedisToSC();
//		importRedisToSC.index();
//	}

	public void index() {
		List<String> records = null;
		List<SolrInputDocument> docs = null;
		while (true) {
			docs = new ArrayList<>();
			// 批量Add并Commit
			logger.info("Starting index ...");
			records = redisMq.getRecords("cdh5.cache.records");
			for (String record : records) {
				SolrDocument doc = JsonUtils.getObject(record, SolrDocument.class);
				docs.add(QueryCore.transSolrDocumentToInputDocument(doc));
			}
			if (!docs.isEmpty()) {
				core.addDocToSolr(docs);
			}
			try {
				Thread.sleep(1_000);
			} catch (InterruptedException e) {
				// TODO
			}
			logger.info("Finishing index ...");
		}
	}

	public void close() {
		try {
			core.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		redisMq.close();
	}

}
