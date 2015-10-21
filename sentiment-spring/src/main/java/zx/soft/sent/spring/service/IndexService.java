package zx.soft.sent.spring.service;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import zx.soft.kafka.producer.ProducerInstance;
import zx.soft.sent.common.index.PostData;
import zx.soft.sent.common.index.RecordInfo;
import zx.soft.sent.dao.persist.PersistCore;
import zx.soft.sent.solr.utils.RedisMQ;
import zx.soft.sent.spring.domain.ErrorResponse;
import zx.soft.utils.json.JsonUtils;
import zx.soft.utils.log.LogbackUtil;
import zx.soft.utils.threads.ApplyThreadPool;

/**
 * 索引服务类
 *
 * @author wanggang
 *
 */
@Service
public class IndexService {

	private static Logger logger = LoggerFactory.getLogger(IndexService.class);

	public static final String TOPIC = "sentiment-cache";

	@Inject
	private PersistCore persistCore;

	@Inject
	private RedisMQ redisMQ;

	private static ThreadPoolExecutor pool = ApplyThreadPool.getThreadPoolExector();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				pool.shutdown();
			}
		}));
	}

	public ErrorResponse addIndexData(final PostData postData) {
		if (postData == null) {
			logger.info("Records' size=0");
			return new ErrorResponse.Builder(-1, "no post data.").build();
		}
		logger.info("Records' Size:{}", postData.getRecords().size());
		try {
			if (postData.getRecords().size() > 0) {
				//				System.out.println(JsonUtils.toJson(postData.getRecords()));
				pool.execute(new Thread(new Runnable() {
					@Override
					public void run() {
						// 持久化到Redis
						//						addToRedis(postData.getRecords());
						// 这里面以及包含了错误日志记录
						//						persist(postData.getRecords());
						push2Kafka(postData.getRecords());
					}
				}));
			}
			return new ErrorResponse.Builder(0, "ok").build();
		} catch (Exception e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
			return new ErrorResponse.Builder(-1, "persist error!").build();
		}
	}

	/**
	 * 数据持久化到Redis
	 */
	private void addToRedis(List<RecordInfo> records) {
		String[] data = new String[records.size()];
		for (int i = 0; i < records.size(); i++) {
			if (records.get(i).getPic_url().length() > 500) {
				records.get(i).setPic_url(records.get(i).getPic_url().substring(0, 500));
			}
			data[i] = JsonUtils.toJsonWithoutPretty(records.get(i));
		}
		try {
			redisMQ.addRecord(data);
		} catch (Exception e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
		}
	}

	/**
	 * 数据持久化到Mysql
	 */
	private void persist(List<RecordInfo> records) {
		for (RecordInfo record : records) {
			if (record.getPic_url().length() > 500) {
				record.setPic_url(record.getPic_url().substring(0, 500));
			}
			persistCore.persist(record);
		}
	}

	private void push2Kafka(List<RecordInfo> records) {
		ProducerInstance instance = ProducerInstance.getInstance();
		for (RecordInfo record : records) {
			instance.pushRecord(TOPIC, JsonUtils.toJsonWithoutPretty(record));
		}
	}

}