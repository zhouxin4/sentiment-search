package zx.soft.sent.dao.firstpage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.dao.riak.RiakClientInstance;
import zx.soft.utils.log.LogbackUtil;

/**
 * OA首页查询信息
 *
 * @author wanggang
 *
 */
public class RiakFirstPageHarmful {

	private static Logger logger = LoggerFactory.getLogger(RiakFirstPageHarmful.class);

	private final RiakClientInstance riakClient;

	public RiakFirstPageHarmful() {
		try {
			riakClient = new RiakClientInstance();
		} catch (RuntimeException e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
			throw new RuntimeException(e);
		}
	}

	/**
	 * 插入OA首页查询数据
	 */
	public void insertFirstPage(int type, String timestr, String result) {
		riakClient.writeString("default", type + "", timestr, result);
	}

	/**
	 * 查询OA首页查询数据
	 */
	public String selectFirstPage(int type, String timestr) {
		return new String(riakClient.readString("default", type + "", timestr).getValue().getValue());
	}

	/**
	 * 删除OA首页查询数据
	 */
	public void deleteFirstPage(int type, String timestr) {
		riakClient.deleteObject("default", type + "", timestr);
	}

	public void close() {
		if (riakClient != null) {
			riakClient.close();
		}
	}

}
