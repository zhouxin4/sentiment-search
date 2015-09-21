package zx.soft.sent.solr.origin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.dao.domain.allinternet.InternetTask;
import zx.soft.sent.dao.insight.RiakInsight;
import zx.soft.sent.dao.oracle.OracleJDBC;
import zx.soft.sent.solr.query.QueryCore;
import zx.soft.sent.solr.utils.RedisMQ;
import zx.soft.utils.checksum.CheckSumUtils;
import zx.soft.utils.log.LogbackUtil;
import zx.soft.utils.time.TimeUtils;

/**
 * 读取Oracle数据库中的信息溯源任务信息，并查询结果存储缓存信息
 * old: 缓存在riak中。 运行目录：  /run-work/timer/origin/sentiment-solr
 * new: 缓存在cdh5solr中 运行目录： /run-work/timer/origin_new/sentiment-solr
 * 启动  ： bin/originupdate.sh
 * @author donglei
 *
 */
public class OriginUpdate {

	private static Logger logger = LoggerFactory.getLogger(OriginUpdate.class);

	// 读取Oracle中任务信息类
	private OracleJDBC oracleJDBC;
	// 搜索查询类
	private QueryCore search;
	// 持久化类
	private RiakInsight riakAccess;
	// 缓存Redis
	private RedisMQ redisMQ;

	// 查询需要更新缓存信息的任务
	public static final String QUERY_EXECUTED = "select id,cjzid,cjsj,gjc,jssj,rwzt from jhrw_rwdd where bz=1 and gsfl=5 and rwzt in (0, 1)";

	public OriginUpdate() {
		this.oracleJDBC = new OracleJDBC();
		this.search = QueryCore.getInstance();
		this.riakAccess = new RiakInsight();
		this.redisMQ = new RedisMQ();
	}

	public static void main(String[] args) throws SQLException {
		OriginUpdate taskUpdate = new OriginUpdate();
		taskUpdate.tackleExecutedTasks();
		taskUpdate.riakAccess.close();
		taskUpdate.search.close();
		taskUpdate.oracleJDBC.close();
	}

	/**
	 * 获取需要更新的任务
	 */
	public void tackleExecutedTasks() {
		logger.info("Updating executed tasks ...");
		tackleTasks(QUERY_EXECUTED);
	}

	private void tackleTasks(String query) {
		HashMap<String, InternetTask> tasks;
		try {
			tasks = getTasks(query);
			if (tasks == null) {
				return;
			}
			logger.info("Updating tasks' size={}", tasks.size());
			ExecutorService executor = Executors.newFixedThreadPool(5);
			for (Entry<String, InternetTask> tmp : tasks.entrySet()) {
				//				executor.execute(new OriginUpdateRunnable(search, riakAccess, tmp.getValue()));
				executor.execute(new OriginCacheRedisRunnable(search, redisMQ, tmp.getValue()));
			}
			executor.shutdown();
			while (!executor.isTerminated()) {
				Thread.sleep(1000);
			}
		} catch (Exception e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
			throw new RuntimeException(e);
		}
	}

	/**
	 * 获取需要更新的任务信息
	 */
	private HashMap<String, InternetTask> getTasks(String query) throws SQLException {
		HashMap<String, InternetTask> result = new HashMap<>();
		ResultSet rs = oracleJDBC.query(query);
		if (rs == null) {
			logger.info("No tasks!");
			return null;
		}
		InternetTask task = null;
		while (rs.next()) {
			String identity = CheckSumUtils.getMD5(
					rs.getString("id") + rs.getString("cjzid")
							+ TimeUtils.transToSolrDateStr(rs.getTimestamp("cjsj").getTime())).toUpperCase();
			boolean needed = true;
			if (rs.getDate("jssj") != null && rs.getDate("jssj").before(new Date())) {
				if (riakAccess.selectHotkeys("origins", identity + "_P1") != null) {
					needed = false;
				}
			}
			if (needed) {
				task = new InternetTask();
				task.setIdentify(identity);
				task.setKeywords(rs.getString("gjc"));
				task.setEnd_time((rs.getDate("jssj") == null) ? TimeUtils.transToSolrDateStr(System.currentTimeMillis())
						: TimeUtils.transToSolrDateStr(rs.getDate("jssj").getTime()));
				result.put(identity, task);
			}

		}
		return result;
	}

	public void close() {
		oracleJDBC.close();
		search.close();
	}

}
