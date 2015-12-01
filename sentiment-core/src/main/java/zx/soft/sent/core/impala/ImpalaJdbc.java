package zx.soft.sent.core.impala;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.utils.config.ConfigUtil;
import zx.soft.utils.log.LogbackUtil;

public class ImpalaJdbc {

	private static Logger logger = LoggerFactory.getLogger(ImpalaJdbc.class);

	private Connection conn;

	public ImpalaJdbc() {

		Properties props = ConfigUtil.getProps("hive-conn.properties");
		try {
			Class.forName(props.getProperty("jdbc.driver.class.name"));
			conn = DriverManager.getConnection(props.getProperty("connection.url"));
		} catch (ClassNotFoundException | SQLException e) {
			logger.error(LogbackUtil.expection2Str(e));
			throw new RuntimeException();
		}
	}

	public ResultSet Query(String sqlStatement) throws SQLException {
		PreparedStatement statement = conn.prepareStatement(sqlStatement);

		ResultSet result = statement.executeQuery();
		return result;
	}

	public int update(String sqlStatement) throws SQLException {
		PreparedStatement statement = conn.prepareStatement(sqlStatement);
		int updateCount = statement.executeUpdate(sqlStatement);
		return updateCount;

	}

	public Connection getConnection() {
		return conn;
	}

	public void closeConnection() {
		try {
			conn.close();
		} catch (SQLException e) {
			logger.info(LogbackUtil.expection2Str(e));
		}
	}
}
