package zx.soft.sent.common.insight;

/**
 * @author donglei
 */
public class HbaseConstant {

	// 用户关系表
	public static final String TABLE_NAME = "user_relation";

	// hive映射表
	public static final String HIVE_TABLE = "parquet_compression.user_relat";

	// 列簇
	public static final String FAMILY_NAME = "cf";

	// 真实用户名
	public static final String TRUE_USER = "tu";

	// 真实用户的虚拟用户名
	public static final String VIRTUAL = "vu";

	// 虚拟用户发帖时间
	public static final String TIMESTAMP = "ts";

	// 虚拟用户平台类型
	public static final String PLATFORM = "pl";

	// 虚拟用户网站类型
	public static final String SOURCE_ID = "sid";

	// 虚拟用户发帖solr存储ID
	public static final String ID = "id";

	// 虚拟用户发帖搜索字段
	public static final String TEXT = "tx";

	// 发帖完整record
	//	public static final String COMPLETE_RECORD = "cr";

	// 评论虚拟用户
	public static final String COMMENT_USER = "cu";

	// 评论时间
	public static final String COMMENT_TIME = "ct";

	// 评论内容
	public static final String COMMENT_CONTEXT = "cc";

}
