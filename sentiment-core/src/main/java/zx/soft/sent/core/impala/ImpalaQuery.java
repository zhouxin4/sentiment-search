package zx.soft.sent.core.impala;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ImpalaQuery {

	private static ImpalaJdbc impala = new ImpalaJdbc();

	//获取活跃用户
	public static List<String> getTopNActiveUser(int from) throws SQLException {
		String query = "SELECT key FROM user_relat ORDER BY " + "ts" + " DESC LIMIT 1000 OFFSET " + from;
		ResultSet result = impala.Query(query);
		List<String> topN = new ArrayList<>();
		while (result.next()) {
			topN.add(result.getString(1));
		}
		return topN;
	}

	//从最近爬取的微博表active_user_lastest_weibos中获取最大的微博id,以便在循环爬取活跃用户最新微博时进行since_id更新
	public static String getMaxId() throws SQLException {
		String relationSql = "SELECT  `true_user_id` , COUNT(`true_user_id` ) AS num FROM  `virtual_user` "
				+ "WHERE web_type =3 AND lasttime BETWEEN  '2015-07-27 17:50:00' AND  '2015-07-29 17:50:00'"
				+ "GROUP BY  `true_user_id` ORDER BY num DESC  LIMIT 10";
		ResultSet result = impala.Query(relationSql);
		while (result.next()) {
			System.out.println(result.getString(0));
			System.out.println(result.getString("num"));
		}
		return null;
	}

	public static void getRelation(String trueUser) throws SQLException {
		String sql = "select * from user_relat where tu=\'" + trueUser + "\'";
		ResultSet result = impala.Query(sql);
		while (result.next()) {
			System.out.println(result.getString(1));
		}
	}

	public static void getTotalCount() throws SQLException {
		String sql = "select count(*) AS num from user_relat";
		ResultSet result = impala.Query(sql);
		while (result.next()) {
			System.out.println(result.getInt("num"));
		}
	}

	public static void main(String[] args) throws SQLException {
		//		List<String> topN = ImpalaQuery.getTopNActiveUser(0);
		//		System.out.println(topN.size());
		//		System.out.println(ImpalaQuery.getMaxId());
		final ImpalaConnPool pool = ImpalaConnPool.getPool(5, 10);
		for (int i = 0; i < 50; i++) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					String sql = "select count(*) AS num from user_relat";
					try {
						ImpalaJdbc jdbc = pool.checkOut();
						ResultSet result = jdbc.Query(sql);
						while (result.next()) {
							System.out.println(result.getInt("num"));
						}
						result.close();
						pool.checkIn(jdbc);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}).start();
		}
		getRelation("alibaba");
		getTotalCount();
	}
}
