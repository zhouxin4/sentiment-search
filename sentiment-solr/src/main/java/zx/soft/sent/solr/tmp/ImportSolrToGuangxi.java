package zx.soft.sent.solr.tmp;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.common.index.RecordInfo;
import zx.soft.sent.dao.common.MybatisConfig.ServerEnum;
import zx.soft.sent.dao.domain.sentiment.RecordSelect;
import zx.soft.sent.dao.sentiment.SentimentRecord;
import zx.soft.utils.http.HttpClientDaoImpl;
import zx.soft.utils.json.JsonUtils;
import zx.soft.utils.time.TimeUtils;

public class ImportSolrToGuangxi {
	private static final String BASE_URL = "http://121.31.12.34:28985/solr2lucene/data";

	public static void main(String[] args) {
		SentimentRecord sentRecord = new SentimentRecord(ServerEnum.sentiment);
		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
		ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 6, 1, TimeUnit.MINUTES, queue);

		for (int i = 0; i < 128; i++) {
			executor.execute(new DumpRunnable(sentRecord, "sent_records_" + i));
		}
		executor.shutdown();

	}

	public static class DumpRunnable implements Runnable {
		private Logger logger = LoggerFactory.getLogger(DumpRunnable.class);
		private String tableName;
		private SentimentRecord sentRecord;

		public DumpRunnable(SentimentRecord sentRecord, String tableName) {
			this.tableName = tableName;
			this.sentRecord = sentRecord;
		}

		@Override
		public void run() {
			long current = System.currentTimeMillis();
			long low = TimeUtils.transCurrentTime(current, 0, 0, 0, -1);
			List<RecordSelect> records = sentRecord
					.selectRecordsByLasttime(tableName, new Date(low), new Date(current));
			for (RecordSelect record : records) {
				int platform = record.getPlatform();
				if (platform == 1 || platform == 3 || platform == 4 || platform == 5) {
					RecordInfo info = parseRecord(record);
					String response = new HttpClientDaoImpl().doPost(BASE_URL, JsonUtils.toJsonWithoutPretty(info));
					logger.info(record.getId() + "-----" + response);
				}
			}

		}

		public RecordInfo parseRecord(RecordSelect record) {
			if (record == null) {
				return null;
			}
			RecordInfo info = new RecordInfo();
			Field[] fields = record.getClass().getDeclaredFields();
			Class<?> infoClass = info.getClass();
			for (Field field : fields) {
				try {
					field.setAccessible(true);
					Field tmp = null;
					try {
						tmp = infoClass.getDeclaredField(field.getName());
					} catch (NoSuchFieldException | SecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (tmp != null) {
						tmp.setAccessible(true);
						if (field.get(record) instanceof Date) {
							tmp.set(info, ((Date) field.get(record)).getTime());
						} else {
							tmp.set(info, field.get(record));
						}
					}
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return info;

		}

	}
}
