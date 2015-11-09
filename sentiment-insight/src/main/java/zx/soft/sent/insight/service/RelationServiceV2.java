package zx.soft.sent.insight.service;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import zx.soft.sent.common.domain.QueryParams;
import zx.soft.sent.common.insight.HbaseConstant;
import zx.soft.sent.core.impala.ImpalaConnPool;
import zx.soft.sent.core.impala.ImpalaJdbc;
import zx.soft.sent.insight.domain.BlogResponse;
import zx.soft.sent.insight.domain.RelationRequest;
import zx.soft.sent.insight.domain.RelationRequest.EndPoint;
import zx.soft.sent.insight.domain.ResponseResult;
import zx.soft.sent.solr.domain.QueryResult;
import zx.soft.sent.solr.query.QueryCore;
import zx.soft.utils.json.JsonUtils;
import zx.soft.utils.log.LogbackUtil;
import zx.soft.utils.string.ConcatMethod;
import zx.soft.utils.string.StringConcatHelper;
import zx.soft.utils.string.StringUtils;
import zx.soft.utils.time.TimeUtils;

import com.google.common.primitives.Longs;

/**
 * 关联分析模块
 * @author donglei
 */
@Service
public class RelationServiceV2 {

	private static Logger logger = LoggerFactory.getLogger(RelationServiceV2.class);

	private static ImpalaConnPool impala = ImpalaConnPool.getPool(10, 5);

	// 获取评论信息
	public ResponseResult getPostDetail(RelationRequest request) {
		/**
		 * SELECT cu,ct,cc FROM user_relat
		 * WHERE id='E0DF07621A49C087080A987F96AC3432' AND cu IN ('全球眼光','花小仙女','forwardslash') ORDER BY ct;
		 */
		ResponseResult results = new ResponseResult();
		//		results.setNumFound(getTotalCount(request));
		Callable<Long> count = new CountCallable(request);
		FutureTask<Long> numCount = new FutureTask<>(count);
		new Thread(numCount).start();
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("SELECT " + HbaseConstant.COMMENT_USER + "," + HbaseConstant.COMMENT_TIME + ","
				+ HbaseConstant.COMMENT_CONTEXT + " FROM " + HbaseConstant.HIVE_TABLE + " WHERE ");
		sBuilder.append(generateCONSQL(request));
		sBuilder.append(" ORDER BY " + HbaseConstant.COMMENT_TIME + " LIMIT " + request.getRows() + " OFFSET "
				+ request.getStart());
		ImpalaJdbc jdbc = null;
		try {
			jdbc = impala.checkOut();
		} catch (InterruptedException e1) {
		}
		if (jdbc == null) {
			logger.error("Impala连接请求超时！");
			long numFound = 0;
			try {
				numFound = numCount.get(5, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				logger.error(e.getMessage());
			}
			results.setNumFound(numFound);
			return results;
		}
		try {
			ResultSet result = null;
			try {
				result = jdbc.Query(sBuilder.toString());
				while (result.next()) {
					BlogResponse response = new BlogResponse.ResponseBuilder()
							.responseUser(result.getString(HbaseConstant.COMMENT_USER))
							.responseTime(
									TimeUtils.transToCommonDateStr(Long.parseLong(result
											.getString(HbaseConstant.COMMENT_TIME))))
							.responseContent(result.getString(HbaseConstant.COMMENT_CONTEXT)).build();

					logger.info("record: {}", JsonUtils.toJsonWithoutPretty(response));
					results.addResponse(response);
				}
			} finally {
				if (result != null) {
					result.close();
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			impala.checkIn(jdbc);
		}
		long numFound = 0;
		try {
			numFound = numCount.get(5, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.error(e.getMessage());
		}
		results.setNumFound(numFound);

		return results;

	}

	// 查询重点人员与关系人员之间关系密切的帖文
	public QueryResult getRelationPosts(RelationRequest request) {
		/**
		 *  SELECT id,COUNT(id) AS num FROM user_relat
		 *  where tu = 'b8e21e62cdf77059033bc78318a40c88' AND cu in ('紫夜瑾','刘军Nic','AiLeBoo')
		 *  GROUP BY id ORDER BY num DESC LIMIT 10 OFFSET 0;
		 */
		QueryResult queryResult = new QueryResult();
		queryResult.setNumFound(0l);
		long st = System.currentTimeMillis();
		Callable<Long> count = new CountCallable(request);
		FutureTask<Long> numCount = new FutureTask<>(count);
		new Thread(numCount).start();
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("SELECT " + HbaseConstant.ID + ", COUNT(" + HbaseConstant.ID + ") AS num FROM "
				+ HbaseConstant.HIVE_TABLE + " WHERE ");
		sBuilder.append(generateCONSQL(request));
		sBuilder.append(" GROUP BY id ORDER BY num DESC LIMIT " + request.getRows() + " OFFSET " + request.getStart());
		logger.info("查询发帖: {}", sBuilder.toString());
		ImpalaJdbc jdbc = null;
		try {
			jdbc = impala.checkOut();
		} catch (InterruptedException e1) {
		}
		if (jdbc == null) {
			logger.error("Impala连接请求超时！");
			long numFound = 0;
			try {
				numFound = numCount.get(5, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				logger.error(e.getMessage());
			}
			queryResult.setNumFound(numFound);
			return queryResult;
		}
		long sTime = System.currentTimeMillis();
		StringConcatHelper helper = new StringConcatHelper(ConcatMethod.OR);
		List<String> ids = new ArrayList<>();
		try {
			ResultSet result = null;
			try {
				result = jdbc.Query(sBuilder.toString());
				while (result.next()) {
					String id = result.getString(HbaseConstant.ID);
					logger.info("id: {}", id);
					ids.add(id);
					helper.add(id);
				}
			} finally {
				if (result != null) {
					result.close();
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			impala.checkIn(jdbc);
		}
		logger.info("获取发帖ID: {}", System.currentTimeMillis() - sTime);
		if (!ids.isEmpty()) {
			//			List<Callable<SolrDocument>> calls = new ArrayList<>();
			//		for (String id : ids) {
			//			calls.add(new SolrDocCallable(id));
			//		}
			//		List<SolrDocument> docs = AwesomeThreadPool.runCallables(10, calls);
			QueryParams docParams = new QueryParams();
			docParams.setQ("*:*");
			docParams.setFq("id:(" + helper.getString() + ")");

			QueryResult docResult = QueryCore.getInstance().queryData(docParams, false);
			for (String id : ids) {
				for (SolrDocument doc : docResult.getResults()) {
					if (id.equals(doc.getFieldValue("id"))) {
						queryResult.getResults().add(doc);
						break;
					}
				}
			}

		}

		long numFound = 0;
		try {
			numFound = numCount.get(5, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.error(e.getMessage());
		}
		queryResult.setNumFound(numFound);
		logger.info("获取发帖耗时: {}", System.currentTimeMillis() - st);
		return queryResult;
	}

	// 获取关联用户
	public Map<String, Long> getRelatedNickname(RelationRequest request) {
		Map<String, Long> nickCounts = new HashMap<String, Long>();
		/**
		 * SELECT cu , COUNT( cu ) AS num FROM user_relat
		 * 	where tu='b8e21e62cdf77059033bc78318a40c88' AND sid=7 AND pl = 3 AND ts
		 * 	between 1441462138000 and 1441860739000 GROUP BY cu ORDER BY num DESC  LIMIT 10;
		 */
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("SELECT " + HbaseConstant.COMMENT_USER + ", COUNT(" + HbaseConstant.COMMENT_USER
				+ ") AS num FROM " + HbaseConstant.HIVE_TABLE + " WHERE ");
		sBuilder.append(generateCONSQL(request));
		sBuilder.append(" GROUP BY " + HbaseConstant.COMMENT_USER + " ORDER BY num DESC " + "LIMIT 10");
		ImpalaJdbc jdbc = null;
		try {
			jdbc = impala.checkOut();
		} catch (InterruptedException e1) {
		}
		if (jdbc == null) {
			logger.error("Impala连接请求超时！");
			return nickCounts;
		}
		try {
			ResultSet result = null;
			try {
				result = jdbc.Query(sBuilder.toString());
				while (result.next()) {
					logger.info("nickname: {}, count: {}", result.getString("cu"), result.getLong("num"));
					nickCounts.put(result.getString("cu"), result.getLong("num"));
				}
			} finally {
				if (result != null) {
					result.close();
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			impala.checkIn(jdbc);
		}

		return nickCounts;
	}

	public Object relationAnalysed(RelationRequest request) {
		Map<String, Long> count = getRelatedNickname(request);
		List<Entry<String, Long>> hotRels = new ArrayList<>();
		for (Entry<String, Long> entry : count.entrySet()) {
			hotRels.add(entry);
		}
		Collections.sort(hotRels, new Comparator<Entry<String, Long>>() {

			@Override
			public int compare(Entry<String, Long> o1, Entry<String, Long> o2) {
				return -Longs.compare(o1.getValue(), o2.getValue());
			}
		});
		return hotRels;
	}

	public long getTotalCount(RelationRequest request) {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("SELECT COUNT(");
		if (request.getService().equals(EndPoint.POST)) {
			sBuilder.append("DISTINCT " + HbaseConstant.ID);
		} else {
			sBuilder.append("*");
		}
		sBuilder.append(") AS num FROM " + HbaseConstant.HIVE_TABLE + " WHERE ");
		sBuilder.append(generateCONSQL(request));
		ImpalaJdbc jdbc = null;
		try {
			jdbc = impala.checkOut();
		} catch (InterruptedException e1) {
			logger.error(LogbackUtil.expection2Str(e1));
		}
		if (jdbc == null) {
			logger.error("Impala连接请求超时！");
			return 0;
		}
		try {
			ResultSet result = null;
			try {
				result = jdbc.Query(sBuilder.toString());
				while (result.next()) {
					logger.info("count: {}", result.getLong("num"));
					return result.getLong("num");
				}
			} finally {
				result.close();
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			impala.checkIn(jdbc);
		}
		return 0;
	}

	private static class CountCallable implements Callable<Long> {

		private RelationRequest request;

		public CountCallable(RelationRequest request) {
			this.request = request;
		}

		@Override
		public Long call() throws Exception {
			StringBuilder sBuilder = new StringBuilder();
			sBuilder.append("SELECT COUNT(");
			if (request.getService().equals(EndPoint.POST)) {
				sBuilder.append("DISTINCT " + HbaseConstant.ID);
			} else {
				sBuilder.append("*");
			}
			sBuilder.append(") AS num FROM " + HbaseConstant.HIVE_TABLE + " WHERE ");
			sBuilder.append(generateCONSQL(request));
			logger.info("Count SQL : {}", sBuilder.toString());
			ImpalaJdbc jdbc = null;
			try {
				jdbc = impala.checkOut();
			} catch (InterruptedException e1) {
			}
			if (jdbc == null) {
				logger.error("Impala连接请求超时！");
				return 0L;
			}
			ResultSet result = jdbc.Query(sBuilder.toString());
			try {
				try {
					while (result.next()) {
						logger.info("count: {}", result.getLong("num"));
						return result.getLong("num");
					}
				} finally {
					result.close();
				}
			} catch (Exception e) {
				logger.error(e.getMessage());
			} finally {
				impala.checkIn(jdbc);
			}
			return 0L;
		}
	}

	private static class SolrDocCallable implements Callable<SolrDocument> {

		private static final String SQL = "SELECT cr FROM " + HbaseConstant.HIVE_TABLE + " WHERE id='%s' LIMIT 1";

		private String id;

		public SolrDocCallable(String id) {
			this.id = id;
		}

		@Override
		public SolrDocument call() {
			ImpalaJdbc jdbc = null;
			try {
				jdbc = impala.checkOut();
			} catch (InterruptedException e1) {
			}
			if (jdbc == null) {
				logger.error("Impala连接请求超时！");
				return null;
			}
			String crQuery = String.format(SQL, id);
			SolrDocument record = null;
			try {
				ResultSet crResult = null;
				try {
					crResult = jdbc.Query(crQuery);
					while (crResult.next()) {
						// 暂时保留
						logger.info("record: {}", crResult.getString(HbaseConstant.COMMENT_CONTEXT));
						record = JsonUtils.getObject(crResult.getString(HbaseConstant.COMMENT_CONTEXT),
								SolrDocument.class);
						if (record.getFieldValueMap().get("timestamp") != null) {
							record.setField(
									"timestamp",
									TimeUtils.transToCommonDateStr(TimeUtils.transToSolrDateStr(Long.parseLong(record
											.getFieldValueMap().get("timestamp").toString()))));
						}
						if (record.getFieldValueMap().get("lasttime") != null) {
							record.setField(
									"lasttime",
									TimeUtils.transToCommonDateStr(TimeUtils.transToSolrDateStr(Long.parseLong(record
											.getFieldValueMap().get("lasttime").toString()))));
						}
						if (record.getFieldValueMap().get("first_time") != null) {
							record.setField(
									"first_time",
									TimeUtils.transToCommonDateStr(TimeUtils.transToSolrDateStr(Long.parseLong(record
											.getFieldValueMap().get("first_time").toString()))));
						}
						if (record.getFieldValueMap().get("update_time") != null) {
							record.setField(
									"update_time",
									TimeUtils.transToCommonDateStr(TimeUtils.transToSolrDateStr(Long.parseLong(record
											.getFieldValueMap().get("update_time").toString()))));
						}
					}
				} finally {
					if (crResult != null) {
						crResult.close();
					}
				}

			} catch (Exception e) {
				logger.error(LogbackUtil.expection2Str(e));
			} finally {
				impala.checkIn(jdbc);
			}
			return record;

		}
	}

	private static String generateCONSQL(RelationRequest request) {

		StringBuilder sBuilder = new StringBuilder();
		switch (request.getService()) {
		case POST:
			if (!StringUtils.isEmpty(request.getQ())) {
				sBuilder.append(HbaseConstant.TEXT + " RLIKE " + "'.*" + request.getQ() + ".*'");
			}
		case RELATION:
			if (request.getTrueUserId() != null) {
				if (sBuilder.length() > 0) {
					sBuilder.append(" AND ");
				}
				sBuilder.append(HbaseConstant.TRUE_USER + "='" + request.getTrueUserId() + "'");
			}
			if (request.getPlatform() != -1) {
				sBuilder.append(" AND pl=" + request.getPlatform());
			}
			if (request.getSource_id() != -1) {
				sBuilder.append(" AND sid=" + request.getSource_id());
			}
			String timestamp = request.getTimestamp();
			if (timestamp != null) {
				long endTime = System.currentTimeMillis();
				long startTime = TimeUtils.transCurrentTime(endTime, 0, 0, -6, 0);
				int li = timestamp.indexOf("[");
				int ri = timestamp.indexOf("TO");
				int lf = timestamp.indexOf("]");
				try {
					startTime = TimeUtils.tranSolrDateStrToMilli(timestamp.substring(li + 1, ri).trim());
					endTime = TimeUtils.tranSolrDateStrToMilli(timestamp.substring(ri + 2, lf).trim());
				} catch (Exception e) {
					e.printStackTrace();
				}
				sBuilder.append(" AND ts BETWEEN " + startTime + " AND " + endTime);
			}
			if (!request.getVirtuals().isEmpty()) {
				sBuilder.append(" AND " + HbaseConstant.COMMENT_USER + " IN (");
				for (String vir : request.getVirtuals()) {
					sBuilder.append("'" + vir + "',");
				}
				sBuilder.setLength(sBuilder.length() - 1);
				sBuilder.append(")");
			}
			break;
		case DETAIL:
			if (request.getSolr_id() != null) {
				sBuilder.append(" id='" + request.getSolr_id() + "'");
			}
			if (!request.getVirtuals().isEmpty()) {
				sBuilder.append(" AND " + HbaseConstant.COMMENT_USER + " IN (");
				for (String vir : request.getVirtuals()) {
					sBuilder.append("'" + vir + "',");
				}
				sBuilder.setLength(sBuilder.length() - 1);
				sBuilder.append(")");
			}
			break;

		}
		;

		return sBuilder.toString();
	}
}
