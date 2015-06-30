package zx.soft.sent.dao.firstpage;

/**
 * 抽象存储接口
 * @author donglei
 *
 */
public interface FirstPagePersistable {

	/**
	 * 插入OA首页查询数据
	 */
	void insertFirstPage(int type, String timestr, String result);

	/**
	 * 更新OA首页查询数据
	 */
	void updateFirstPage(int type, String timestr, String result);

	/**
	 * 删除OA首页查询数据
	 */
	String selectFirstPage(int type, String timestr);

	/**
	 * 删除OA首页查询数据
	 */
	void deleteFirstPage(int type, String timestr);

	/**
	 * 关闭相关资源
	 */
	void close();
}
