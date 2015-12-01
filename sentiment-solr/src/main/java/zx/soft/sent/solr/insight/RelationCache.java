package zx.soft.sent.solr.insight;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.common.domain.QueryParams;
import zx.soft.sent.common.insight.AreaCode;
import zx.soft.sent.common.insight.HbaseConstant;
import zx.soft.sent.common.insight.TrueUserHelper;
import zx.soft.sent.common.insight.UserDomain;
import zx.soft.sent.common.insight.Virtuals.Virtual;
import zx.soft.sent.core.hbase.HBaseUtils;
import zx.soft.sent.core.hbase.HbaseDao;
import zx.soft.sent.solr.domain.QueryResult;
import zx.soft.sent.solr.query.QueryCore;
import zx.soft.utils.checksum.CheckSumUtils;
import zx.soft.utils.log.LogbackUtil;
import zx.soft.utils.time.TimeUtils;

public class RelationCache {

	private static Logger logger = LoggerFactory.getLogger(RelationCache.class);
	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				HBaseUtils.close();
			}
		});
	}

	public RelationCache() {

	}

	public static void main(String[] args) {
		RelationCache relationCache = new RelationCache();
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
		long endTime = System.currentTimeMillis();
		long startTime = TimeUtils.transCurrentTime(endTime, 0, 0, 0, -1);
		//		String timeFilter = "lasttime:[" + TimeUtils.transToSolrDateStr(startTime) + " TO "
		//				+ TimeUtils.transToSolrDateStr(endTime) + "]";
		String timeFilter = "";
		for (AreaCode area : AreaCode.values()) {
			String areaCode = area.getAreaCode();
			List<UserDomain> trueUsers = TrueUserHelper.getTrueUsers(areaCode);
			for (UserDomain user : trueUsers) {
				String trueUserId = user.getTureUserId();
				List<Virtual> virtuals = TrueUserHelper.getVirtuals(trueUserId);
				for (Virtual virtual : virtuals) {
					if (virtual.getSource_id() == 7) {
						continue;
					}
					try {
						cacheHalfHourRelation(virtual, timeFilter);
					} catch (Exception e) {
						logger.info(LogbackUtil.expection2Str(e));
					}
				}
			}
		}
		// 关闭资源
		QueryCore.getInstance().close();
		logger.info("Finishing query OA-FirstPage data...");
		ImpalaUpdate.default2ParquetTable();
	}

	private void cacheHalfHourRelation(Virtual virtual, String timeFilter) throws Exception {
		QueryParams params = new QueryParams();
		params.setQ("*:*");
		params.setFq(timeFilter);
		params.setFq(params.getFq() + ";" + "(nickname:\"" + virtual.getNickname() + "\" AND source_id:"
				+ virtual.getSource_id() + ")");
		params.setRows(200);
		QueryResult result = QueryCore.getInstance().queryData(params, false);
		cacheOneBlogRelation(result, virtual);
		long numFound = result.getNumFound();
		for (int i = 200; i < numFound; i += 200) {
			params.setStart(i);
			params.setRows(200);
			result = QueryCore.getInstance().queryData(params, false);
			cacheOneBlogRelation(result, virtual);
		}

	}

	private void cacheOneBlogRelation(QueryResult result, Virtual virtual) {
		QueryParams params = new QueryParams();
		params.setQ("*:*");
		params.setRows(200);
		for (SolrDocument doc : result.getResults()) {
			params.setFq("original_id:" + doc.getFieldValue("id").toString());
			QueryResult tmp = QueryCore.getInstance().queryData(params, false);
			for (SolrDocument document : tmp.getResults()) {
				//				if (doc.getFieldValue("nickname").toString().equals(document.getFieldValue("nickname").toString()))
				//					continue;
				logger.info("存入关系：blog(" + doc.getFieldValue("id").toString() + ") --> comment:("
						+ document.getFieldValue("id").toString() + ")");
				HbaseDao dao = new HbaseDao(HbaseConstant.TABLE_NAME, 10);
				byte[] rowKey = CheckSumUtils.md5sum(virtual.getTrueUser() + document.getFieldValue("id").toString());

				dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.TRUE_USER, virtual.getTrueUser());
				dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.TIMESTAMP,
						TimeUtils.transTimeLong(doc.getFieldValue("timestamp").toString()) + "");
				dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.VIRTUAL,
						doc.getFieldValue("nickname").toString());
				dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.PLATFORM,
						doc.getFieldValue("platform").toString());
				dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.SOURCE_ID,
						doc.getFieldValue("source_id").toString());
				dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.ID, doc.getFieldValue("id")
						.toString());
				dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.TEXT,
						(doc.getFieldValue("title") == null ? "" : doc.getFieldValue("title").toString())
								+ "            " + doc.getFieldValue("content").toString());
				//				dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.COMPLETE_RECORD,
				//						JsonUtils.toJsonWithoutPretty(weibo));
				try {
					dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.COMMENT_USER, document
							.getFieldValue("nickname").toString());
				} catch (Exception e) {
				}
				dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.COMMENT_TIME,
						TimeUtils.transTimeLong(document.getFieldValue("timestamp").toString()) + "");
				dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.COMMENT_CONTEXT, document
						.getFieldValue("content").toString());
				dao.addSingleColumn(rowKey, HbaseConstant.FAMILY_NAME, HbaseConstant.FOLLOW_TYPE, "0");
				dao.flushPuts();
			}
		}

	}

	private long getStartTime(long currentTime, int gapMin) {
		Calendar date = Calendar.getInstance();
		date.setTimeInMillis(currentTime);
		date.set(Calendar.MINUTE, date.get(Calendar.MINUTE) + gapMin);
		return date.getTimeInMillis();
	}

}
