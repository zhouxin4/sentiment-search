package zx.soft.sent.solr.insight;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.common.insight.HbaseConstant;
import zx.soft.sent.core.impala.ImpalaJdbc;
import zx.soft.utils.log.LogbackUtil;

public class ImpalaUpdate {
	private static Logger logger = LoggerFactory.getLogger(ImpalaUpdate.class);

	public static void main(String[] args) {

	}

	public static void default2ParquetTable() {
		ImpalaJdbc impala = null;
		try {
			impala = new ImpalaJdbc();
			int impalaCount = getRowCount(impala);
			overwriteTable(impala);
			statsAnalyze(impala);
			int impalaCount_n = getRowCount(impala);
			if (impalaCount_n < impalaCount) {
				logger.error("hive转存impala失败");
			}
		} finally {
			if (impala != null) {
				impala.closeConnection();
			}
		}
	}

	public static int getRowCount(ImpalaJdbc impala) {
		/**
		 * "select count(*) from parquet_compression.user_rel_parquet"
		 */
		String sql = "select COUNT(*) from " + HbaseConstant.IMPALA_TABLE;
		try (ResultSet result = impala.Query(sql)) {
			while (result.next()) {
				logger.info("Impala count : {}", result.getInt(1));
				return result.getInt(1);
			}
		} catch (SQLException e) {
			logger.error(LogbackUtil.expection2Str(e));
		}
		return 0;
	}

	public static void overwriteTable(ImpalaJdbc impala) {
		/**
		 * insert overwrite parquet_compression.user_rel_parquet
		 * 		select rowkey,tu,vu,ts,pl,sid,id,tx,cu,ct,cc,ft from default.user_relat
		 */
		String sql = "insert overwrite " + HbaseConstant.IMPALA_TABLE + " select " + HbaseConstant.ROWKEY + ","
				+ HbaseConstant.TRUE_USER + "," + HbaseConstant.VIRTUAL + "," + HbaseConstant.TIMESTAMP + ","
				+ HbaseConstant.PLATFORM + "," + HbaseConstant.SOURCE_ID + "," + HbaseConstant.ID + ","
				+ HbaseConstant.TEXT + "," + HbaseConstant.COMMENT_USER + "," + HbaseConstant.COMMENT_TIME + ","
				+ HbaseConstant.COMMENT_CONTEXT + "," + HbaseConstant.FOLLOW_TYPE + " from " + HbaseConstant.HIVE_TABLE;

		try {
			int queryUpdate = impala.update(sql);
			logger.info("{}  result :  {}", sql, queryUpdate);
		} catch (SQLException e) {
			logger.error(LogbackUtil.expection2Str(e));
		}
	}

	public static void statsAnalyze(ImpalaJdbc impala) {
		/**
		 * "compute stats parquet_compression.user_rel_parquet"
		 */
		String sql = "compute stats " + HbaseConstant.IMPALA_TABLE;
		try {
			int queryUpdate = impala.update(sql);
			logger.info("{}  result :  {}", sql, queryUpdate);
		} catch (SQLException e) {
			logger.error(LogbackUtil.expection2Str(e));
		}
	}

}
