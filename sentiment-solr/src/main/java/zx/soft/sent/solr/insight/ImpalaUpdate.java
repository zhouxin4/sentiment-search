package zx.soft.sent.solr.insight;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.core.impala.ImpalaJdbc;
import zx.soft.utils.log.LogbackUtil;

public class ImpalaUpdate {
	private static Logger logger = LoggerFactory.getLogger(ImpalaUpdate.class);
	public static final String SQL = "insert overwrite parquet_compression.user_rel_parquet select rowkey,tu,vu,ts,pl,sid,id,tx,cu,ct,cc,ft from default.user_relat";

	public static void main(String[] args) {

	}

	public static void impalaupdate() {
		ImpalaJdbc impala = null;
		try {
			String sql = "compute stats parquet_compression.user_rel_parquet";
			String sql1 = "select count(*) from parquet_compression.user_rel_parquet";
			impala = new ImpalaJdbc();
			int queryUpdate = impala.update(SQL);
			int update = impala.update(sql);
			ResultSet count = impala.Query(sql1);
			logger.info("Update result : {}", queryUpdate);
			logger.info("Update result : {}", update);
			while (count.next()) {
				logger.info("Impala count : {}", count.getInt(1));
			}
		} catch (SQLException e) {
			logger.error(LogbackUtil.expection2Str(e));
		} finally {
			if (impala != null) {
				impala.closeConnection();
			}
		}

	}

}
