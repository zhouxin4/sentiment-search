package zx.soft.sent.insight.service;

import java.sql.ResultSet;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import zx.soft.sent.common.insight.HbaseConstant;
import zx.soft.sent.core.impala.ImpalaConnPool;
import zx.soft.sent.core.impala.ImpalaJdbc;
import zx.soft.sent.insight.domain.BlogResponse;
import zx.soft.sent.insight.domain.RelationRequest;
import zx.soft.sent.solr.domain.QueryResult;
import zx.soft.utils.json.JsonUtils;
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

	private static ImpalaConnPool impala = ImpalaConnPool.getPool(5, 5);

	// 获取评论信息
	public List<BlogResponse> getPostDetail(RelationRequest request) {
		/**
		 * SELECT cu,ct,cc FROM user_relat
		 * WHERE id='E0DF07621A49C087080A987F96AC3432' AND cu IN ('全球眼光','花小仙女','forwardslash') ORDER BY ct;
		 */
		List<BlogResponse> responses = new ArrayList<>();
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
			return responses;
		}
		ResultSet result = jdbc.Query(sBuilder.toString());
		try {
			try {
				while (result.next()) {
					BlogResponse response = new BlogResponse.ResponseBuilder()
							.responseUser(result.getString(HbaseConstant.COMMENT_USER))
							.responseTime(result.getString(HbaseConstant.COMMENT_TIME))
							.responseContent(result.getString(HbaseConstant.COMMENT_CONTEXT)).build();

					logger.info("record: {}", JsonUtils.toJsonWithoutPretty(response));
					responses.add(response);
				}
			} finally {
				result.close();
				impala.checkIn(jdbc);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		return responses;

	}

	// 查询重点人员与关系人员之间关系密切的帖文
	public QueryResult getRelationPosts(RelationRequest request) {
		/**
		 *  SELECT id,COUNT(id) AS num FROM user_relat
		 *  where tu = 'b8e21e62cdf77059033bc78318a40c88' AND cu in ('紫夜瑾','刘军Nic','AiLeBoo')
		 *  GROUP BY id ORDER BY num DESC LIMIT 10 OFFSET 0;
		 */
		QueryResult queryResult = new QueryResult();
		long numFound = getTotalCount(request);
		queryResult.setNumFound(numFound);
		if (numFound == 0) {
			return queryResult;
		}
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("SELECT " + HbaseConstant.ID + ", COUNT(" + HbaseConstant.ID + ") AS num FROM "
				+ HbaseConstant.HIVE_TABLE + " WHERE ");
		sBuilder.append(generateCONSQL(request));
		sBuilder.append(" GROUP BY id ORDER BY num DESC LIMIT 10 OFFSET 0");
		ImpalaJdbc jdbc = null;
		try {
			jdbc = impala.checkOut();
		} catch (InterruptedException e1) {
		}
		if (jdbc == null) {
			logger.error("Impala连接请求超时！");
			return queryResult;
		}
		ResultSet result = jdbc.Query(sBuilder.toString());
		List<String> ids = new ArrayList<>();
		try {
			try {
				while (result.next()) {
					logger.info("id: {}", result.getString(HbaseConstant.ID));
					ids.add(result.getString(HbaseConstant.ID));
				}
			} finally {
				result.close();
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		if (ids.isEmpty()) {
			impala.checkIn(jdbc);
			return queryResult;
		}
		String sql = "SELECT DISTINCT cr FROM " + HbaseConstant.HIVE_TABLE + " WHERE id='%s'";
		for (String id : ids) {
			String crQuery = String.format(sql, id);
			ResultSet crResult = jdbc.Query(crQuery);
			try {
				try {
					while (crResult.next()) {
						logger.info("record: {}", crResult.getString(HbaseConstant.COMPLETE_RECORD));
						SolrDocument record = JsonUtils.getObject(crResult.getString(HbaseConstant.COMPLETE_RECORD),
								SolrDocument.class);
						queryResult.getResults().add(record);
					}
				} finally {
					crResult.close();
				}
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		impala.checkIn(jdbc);
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
		ResultSet result = jdbc.Query(sBuilder.toString());
		try {
			try {
				while (result.next()) {
					logger.info("nickname: {}, count: {}", result.getString("cu"), result.getLong("num"));
					nickCounts.put(result.getString("cu"), result.getLong("num"));
				}
			} finally {
				result.close();
				impala.checkIn(jdbc);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
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
		if (hotRels.size() < 10) {
			return hotRels;
		}

		return hotRels.subList(0, 10);
	}

	public long getTotalCount(RelationRequest request) {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("SELECT COUNT(*) AS num FROM " + HbaseConstant.HIVE_TABLE + " WHERE ");
		sBuilder.append(generateCONSQL(request));
		ImpalaJdbc jdbc = null;
		try {
			jdbc = impala.checkOut();
		} catch (InterruptedException e1) {
		}
		if (jdbc == null) {
			logger.error("Impala连接请求超时！");
			return 0;
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
				impala.checkIn(jdbc);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return 0;
	}

	private String generateCONSQL(RelationRequest request) {

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
					long lTime = TimeUtils.tranSolrDateStrToMilli(timestamp.substring(li + 1, ri).trim());
					startTime = TimeUtils.getZeroHourTime(lTime);
					long rTime = TimeUtils.tranSolrDateStrToMilli(timestamp.substring(ri + 2, lf).trim());
					endTime = TimeUtils.getZeroHourTime(rTime);
				} catch (ParseException e) {
					e.printStackTrace();
				}
				sBuilder.append(" AND ts BETWEEN" + startTime + "AND" + endTime);
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
