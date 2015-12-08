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

		//		for (AreaCode area : AreaCode.values()) {
		//			String areaCode = area.getAreaCode();
		//			List<UserDomain> trueUsers = TrueUserHelper.getTrueUsers(areaCode);
		//			for (UserDomain user : trueUsers) {
		//				String trueUserId = user.getTureUserId();
		//				long currentTime = System.currentTimeMillis();
		//				currentTime = TimeUtils.transCurrentTime(currentTime, 0, -2, 0, 0);
		//				for (int i = 0; i < 24 * 30 * 2; i++) {
		//					long tmp = TimeUtils.transCurrentTime(currentTime, 0, 0, 0, -i);
		//					String words = insight.selectHotkeys("hotkeys", trueUserId + "_" + TimeUtils.timeStrByHour(tmp));
		//					if (words != null) {
		//						insight.deleteHotkeys("hotkeys", trueUserId + "_" + TimeUtils.timeStrByHour(tmp));
		//					}
		//				}
		//			}
		//		}

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
