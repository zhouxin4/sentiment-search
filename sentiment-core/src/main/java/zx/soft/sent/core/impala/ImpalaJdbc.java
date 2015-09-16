package zx.soft.sent.core.impala;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import zx.soft.utils.config.ConfigUtil;

public class ImpalaJdbc {

	private Connection conn;

	public ImpalaJdbc() {

		Properties props = ConfigUtil.getProps("hive-conn.properties");
		try {
			Class.forName(props.getProperty("jdbc.driver.class.name"));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		try {
			conn = DriverManager.getConnection(props.getProperty("connection.url"));
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public ResultSet Query(String sqlStatement) throws SQLException {
		PreparedStatement statement = conn.prepareStatement(sqlStatement);
		ResultSet result = statement.executeQuery();
		return result;
	}

	public Connection getConnection() {
		return conn;
	}

	public void closeConnection() {
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
