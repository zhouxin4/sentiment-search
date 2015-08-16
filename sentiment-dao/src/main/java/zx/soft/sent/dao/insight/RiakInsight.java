package zx.soft.sent.dao.insight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.core.riak.RiakInstance;
import zx.soft.utils.log.LogbackUtil;

import com.basho.riak.client.core.query.RiakObject;

/**
 * OA首页查询信息
 * @author donglei
 *
 */
public class RiakInsight {

	private static Logger logger = LoggerFactory.getLogger(RiakInsight.class);

	private final RiakInstance riakClient;

	public RiakInsight() {
		try {
			riakClient = new RiakInstance();
		} catch (RuntimeException e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		final RiakInsight insight = new RiakInsight();
		//		long start = System.currentTimeMillis();
		//		List<Callable<String>> lists = new ArrayList<Callable<String>>();
		//		for (int i = 0; i < 500; i++) {
		//			lists.add(new Callable<String>() {
		//
		//				@Override
		//				public String call() throws Exception {
		//					// TODO Auto-generated method stub
		//					return insight.selectHotkeys("hotkeys", "34232a86c69edcf1e86f3caefcbed9d6_2015-07-29,19");
		//				}
		//
		//			});
		//		}
		//		AwesomeThreadPool.runCallables(5, lists);
		//		logger.info("获取keys耗时: {}", System.currentTimeMillis() - start);
		insight.deleteHotkeys("hotkeys", "86abe49fae4ad9eb07ecb765bf611209_2015-08-10,16");
		insight.close();
	}

	/**
	 * 插入重点人员数据数据
	 */
	public void insertHotkeys(String type, String key, String result) {
		riakClient.writeString("default", type, key, result);
	}

	/**
	 * 查询重点人员数据
	 */
	public String selectHotkeys(String type, String key) {
		RiakObject riakObject = riakClient.readString("default", type, key);
		if (riakObject == null) {
			return null;
		}
		return new String(riakObject.getValue().getValue());
	}

	/**
	 * 删除OA首页查询数据
	 */
	public void deleteHotkeys(String type, String key) {
		riakClient.deleteObject("default", type, key);
	}

	public void close() {
		if (riakClient != null) {
			riakClient.close();
		}
	}

	public void updateHotkey(int type, String key, String result) {
		// TODO Auto-generated method stub

	}

}
