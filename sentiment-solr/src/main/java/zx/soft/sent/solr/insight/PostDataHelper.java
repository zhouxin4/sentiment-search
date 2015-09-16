package zx.soft.sent.solr.insight;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.solr.utils.RedisMQ;

/**
 * @author donglei
 */
public class PostDataHelper {

	private static final Logger logger = LoggerFactory.getLogger(PostDataHelper.class);

	private List<String> docs = null;
	private RedisMQ redisMQ = null;
	private static PostDataHelper helper = new PostDataHelper();

	private PostDataHelper() {
		this.docs = new ArrayList<>();
		redisMQ = new RedisMQ();
	}

	public static PostDataHelper getInstance() {
		return helper;
	}

	public synchronized void addRecord(String jsonRecord) {
		docs.add(jsonRecord);
		if (docs.size() > 10) {
			flush();
		}
	}

	public synchronized void flush() {
		String[] arDocs = this.docs.toArray(new String[this.docs.size()]);
		docs.clear();
		redisMQ.addRecord(arDocs);
		logger.info("存入Redis, {}条数据", arDocs.length);
	}

}
