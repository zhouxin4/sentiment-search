package zx.soft.sent.solr.insight;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.common.index.RecordInfo;
import zx.soft.sent.common.insight.AreaCode;
import zx.soft.sent.common.insight.HbaseConstant;
import zx.soft.sent.common.insight.TrueUserHelper;
import zx.soft.sent.common.insight.UserDomain;
import zx.soft.sent.common.insight.Virtuals.Virtual;
import zx.soft.sent.core.hbase.HBaseUtils;
import zx.soft.sent.core.hbase.HbaseDao;
import zx.soft.sent.solr.query.QueryCore;
import zx.soft.utils.checksum.CheckSumUtils;
import zx.soft.utils.codec.URLCodecUtils;
import zx.soft.utils.http.HttpClientDaoImpl;
import zx.soft.utils.json.JsonUtils;
import zx.soft.utils.string.StringUtils;
import zx.soft.weibo.sina.common.WidToMid;

public class RelationCacheV2 {

	private static Logger logger = LoggerFactory.getLogger(RelationCacheV2.class);

	private static String LASTEST_BLOGS = "http://192.168.32.22:8888/weibos/lastest?screen_name=%s";
	private static String COMMENTS = "http://192.168.32.22:8888/weibos/comments/%s";
	private static final String WEIBO_BASE_URL = "http://weibo.com/";

	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				HBaseUtils.close();
			}
		});
	}

	public RelationCacheV2() {

	}

	public static void main(String[] args) {
		RelationCacheV2 relationCache = new RelationCacheV2();
		relationCache.run();
	}

	private void run() {
		logger.info("Starting generate data...");
		try {
			HBaseUtils.createTable(HbaseConstant.TABLE_NAME, new String[] { HbaseConstant.FAMILY_NAME });
		} catch (IOException e) {
			logger.error(e.getMessage());
			throw new RuntimeException();
		}
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/json");
		for (AreaCode area : AreaCode.values()) {
			String areaCode = area.getAreaCode();
			List<UserDomain> trueUsers = TrueUserHelper.getTrueUsers(areaCode);
			for (UserDomain user : trueUsers) {
				String trueUserId = user.getTureUserId();
				List<Virtual> virtuals = TrueUserHelper.getVirtuals(trueUserId);
				for (Virtual virtual : virtuals) {
					// 新浪微博
					if (virtual.getSource_id() == 7) {
						String url = String
								.format(LASTEST_BLOGS, URLCodecUtils.encoder(virtual.getNickname(), "UTF-8"));
						String result = new HttpClientDaoImpl().doGet(url, headers);
						List<Weibo> weibos = JsonUtils.parseJsonArray(result, Weibo.class);
						for (Weibo weibo : weibos) {
							//							logger.info(weibo.toString());
							if (!checkWeibo(weibo)) {
								logger.info("跳过不正常微博ID: {}", weibo.getId());
								continue;
							}
							RecordInfo weiboInfo = transWeiboToRecord(weibo);
							logger.info(weiboInfo.toString());
							PostDataHelper.getInstance().addRecord(JsonUtils.toJsonWithoutPretty(weiboInfo));

							String commentUrl = String.format(COMMENTS, weibo.getId());
							String commentResult = new HttpClientDaoImpl().doGet(commentUrl, headers);
							List<Comment> comments = JsonUtils.parseJsonArray(commentResult, Comment.class);
							for (Comment comment : comments) {
								//								logger.info(comment.toString());
								RecordInfo commentInfo = transCommentToRecord(comment);
								PostDataHelper.getInstance().addRecord(JsonUtils.toJsonWithoutPretty(commentInfo));
								logger.info(commentInfo.toString());
								cacheOneBlogRelation(virtual, weiboInfo, commentInfo);
								logger.info("存储关系: webId({}) -- > commentId({}),originId({})", weiboInfo.getId(),
										commentInfo.getId(), commentInfo.getOriginal_id());
							}
						}
					}
				}
			}
		}
		PostDataHelper.getInstance().flush();
		// 关闭资源
		QueryCore.getInstance().close();
		HBaseUtils.close();
		logger.info("Finishing query OA-FirstPage data...");
	}

	private void cacheOneBlogRelation(Virtual virtual, RecordInfo weibo, RecordInfo comment) {
		HbaseDao dao = new HbaseDao(HbaseConstant.TABLE_NAME, 10);
		//		byte[] md5 = CheckSumUtils.md5sum(virtual.getTrueUser());
		//		byte[] uuid = CheckSumUtils.md5sum(comment.getId());
		//		byte[] rowKey = new byte[CheckSumUtils.MD5_LENGTH * 2];
		//		int offset = 0;
		//		offset = Bytes.putBytes(rowKey, offset, md5, 0, md5.length);
		//		offset = Bytes.putBytes(rowKey, offset, uuid, 0, md5.length);
		byte[] rowKey = CheckSumUtils.md5sum(virtual.getTrueUser() + comment.getId());
		dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.TRUE_USER, virtual.getTrueUser());
		dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.TIMESTAMP, weibo.getTimestamp() + "");
		dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.VIRTUAL, weibo.getNickname());
		dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.PLATFORM, weibo.getPlatform() + "");
		dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.SOURCE_ID, weibo.getSource_id() + "");
		dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.ID, weibo.getId());
		dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.TEXT, weibo.getTitle() + "            "
				+ weibo.getContent());
		//		dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.COMPLETE_RECORD,
		//				JsonUtils.toJsonWithoutPretty(weibo));
		dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.COMMENT_USER, comment.getNickname());
		dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.COMMENT_TIME, comment.getTimestamp() + "");
		dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.COMMENT_CONTEXT, comment.getContent());
		//		Put put = new Put(rowKey);
		//		put.add(Bytes.toBytes(HbaseConstant.FAMILY_NAME), Bytes.toBytes(HbaseConstant.TRUE_USER),
		//				Bytes.toBytes(virtual.getTrueUser()));
		//		put.add(Bytes.toBytes(HbaseConstant.FAMILY_NAME), Bytes.toBytes(HbaseConstant.TIMESTAMP),
		//				Bytes.toBytes(weibo.getTimestamp() + ""));
		//		put.add(Bytes.toBytes(HbaseConstant.FAMILY_NAME), Bytes.toBytes(HbaseConstant.VIRTUAL),
		//				Bytes.toBytes(weibo.getNickname()));
		//		put.add(Bytes.toBytes(HbaseConstant.FAMILY_NAME), Bytes.toBytes(HbaseConstant.PLATFORM),
		//				Bytes.toBytes(weibo.getPlatform() + ""));
		//		put.add(Bytes.toBytes(HbaseConstant.FAMILY_NAME), Bytes.toBytes(HbaseConstant.SOURCE_ID),
		//				Bytes.toBytes(weibo.getSource_id() + ""));
		//		put.add(Bytes.toBytes(HbaseConstant.FAMILY_NAME), Bytes.toBytes(HbaseConstant.ID), Bytes.toBytes(weibo.getId()));
		//		put.add(Bytes.toBytes(HbaseConstant.FAMILY_NAME), Bytes.toBytes(HbaseConstant.TEXT),
		//				Bytes.toBytes(weibo.getTitle() + "            " + weibo.getContent()));
		//		put.add(Bytes.toBytes(HbaseConstant.FAMILY_NAME), Bytes.toBytes(HbaseConstant.COMPLETE_RECORD),
		//				Bytes.toBytes(JsonUtils.toJsonWithoutPretty(weibo)));
		//		put.add(Bytes.toBytes(HbaseConstant.FAMILY_NAME), Bytes.toBytes(HbaseConstant.COMMENT_USER),
		//				Bytes.toBytes(comment.getNickname()));
		//		put.add(Bytes.toBytes(HbaseConstant.FAMILY_NAME), Bytes.toBytes(HbaseConstant.COMMENT_TIME),
		//				Bytes.toBytes(comment.getTimestamp() + ""));
		//		put.add(Bytes.toBytes(HbaseConstant.FAMILY_NAME), Bytes.toBytes(HbaseConstant.COMMENT_CONTEXT),
		//				Bytes.toBytes(comment.getContent()));
		//		HBaseUtils.put(HbaseConstant.TABLE_NAME, put);
		dao.flushPuts();
	}

	public RecordInfo transWeiboToRecord(Weibo weibo) {
		RecordInfo recordInfo = new RecordInfo();
		recordInfo.setPlatform(3);
		recordInfo.setSource_id(7);
		recordInfo.setSource_name("新浪微博");
		recordInfo.setLocation_code(110000);
		recordInfo.setProvince_code(11);
		recordInfo.setFirst_time(System.currentTimeMillis());
		recordInfo.setUpdate_time(System.currentTimeMillis());
		recordInfo.setLasttime(System.currentTimeMillis());
		recordInfo.setIdentify_id(110L); // 表示本地
		recordInfo.setCountry_code(1);
		recordInfo.setIp("180.149.134.141");
		recordInfo.setLocation("北京市 电信集团公司");
		recordInfo.setSource_type(weibo.getSource_type());
		recordInfo.setTimestamp(weibo.getCreated_at().getTime());
		recordInfo.setId((CheckSumUtils.getMD5(WEIBO_BASE_URL + weibo.getUser().getId() + "/"
				+ WidToMid.wid2mid(weibo.getId()))).toUpperCase());
		recordInfo.setUsername(weibo.getUser().getId() + "");
		recordInfo.setNickname(weibo.getUser().getScreen_name());
		recordInfo.setUrl(WEIBO_BASE_URL + weibo.getUser().getId() + "/" + WidToMid.wid2mid(weibo.getId()));
		recordInfo.setContent(weibo.getText());
		//					recordInfo.setLocation(user.getFieldValue("location").toString());
		//					recordInfo.setCity_code(Integer.parseInt(user.getFieldValue("city").toString()));
		//					recordInfo.setProvince_code(Integer.parseInt(user.getFieldValue("province").toString()));
		//					recordInfo.setLocation_code();
		return recordInfo;

	}

	public RecordInfo transCommentToRecord(Comment comment) {
		RecordInfo recordInfo = new RecordInfo();
		recordInfo.setPlatform(7);
		recordInfo.setSource_id(7);
		recordInfo.setSource_name("新浪微博");
		recordInfo.setLocation_code(110000);
		recordInfo.setProvince_code(11);
		recordInfo.setFirst_time(System.currentTimeMillis());
		recordInfo.setUpdate_time(System.currentTimeMillis());
		recordInfo.setLasttime(System.currentTimeMillis());
		recordInfo.setIdentify_id(110L); // 表示本地
		recordInfo.setCountry_code(1);
		recordInfo.setIp("180.149.134.141");
		recordInfo.setLocation("北京市 电信集团公司");
		recordInfo.setSource_type(comment.getSource_type());
		recordInfo.setTimestamp(comment.getCreated_at().getTime());
		recordInfo.setId((CheckSumUtils.getMD5(comment.getOriginal_id() + comment.getId())).toUpperCase());
		recordInfo.setUsername(String.valueOf(comment.getUid()));
		recordInfo.setNickname(comment.getScreen_name());
		recordInfo.setContent(comment.getText());
		recordInfo.setOriginal_id((CheckSumUtils.getMD5(WEIBO_BASE_URL + comment.getOriginal_uid() + "/"
				+ WidToMid.wid2mid(comment.getOriginal_id()))).toUpperCase());
		recordInfo.setOriginal_name(comment.getOriginal_screen_name());
		recordInfo.setOriginal_url(WEIBO_BASE_URL + comment.getOriginal_uid() + "/"
				+ WidToMid.wid2mid(comment.getOriginal_id()));
		recordInfo.setOriginal_uid(String.valueOf(comment.getOriginal_uid()));

		//					recordInfo.setLocation(user.getFieldValue("location").toString());
		//					recordInfo.setCity_code(Integer.parseInt(user.getFieldValue("city").toString()));
		//					recordInfo.setProvince_code(Integer.parseInt(user.getFieldValue("province").toString()));
		//					recordInfo.setLocation_code();
		return recordInfo;
	}

	private long getStartTime(long currentTime, int gapMin) {
		Calendar date = Calendar.getInstance();
		date.setTimeInMillis(currentTime);
		date.set(Calendar.HOUR_OF_DAY, date.get(Calendar.MINUTE) + gapMin);
		return date.getTimeInMillis();
	}

	private boolean checkWeibo(Weibo weibo) {
		if (StringUtils.isEmpty(weibo.getId()) || weibo.getId().length() != 16) {
			return false;
		} else {
			return true;
		}
	}

}
