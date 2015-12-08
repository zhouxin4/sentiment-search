package zx.soft.sent.solr.allinternet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.dao.allinternet.AllInternet;
import zx.soft.sent.dao.common.MybatisConfig;
import zx.soft.sent.dao.domain.allinternet.InternetTask;
import zx.soft.sent.dao.oracle.OracleJDBC;
import zx.soft.sent.solr.query.SearchingData;
import zx.soft.utils.checksum.CheckSumUtils;
import zx.soft.utils.log.LogbackUtil;
import zx.soft.utils.threads.ApplyThreadPool;
import zx.soft.utils.time.TimeUtils;

import com.google.common.base.Joiner;

/**
 * 读取Oracle数据库中的全网任务信息，并查询结果存储缓存信息
 *
 * 广西 ： gxqt4
 * 启动目录： /home/solr/run-work/timer/oa-allinternet
 *
 * @author wanggang
 *
 */
public class TaskUpdate {

	private static Logger logger = LoggerFactory.getLogger(TaskUpdate.class);

	// 线程池
	private static ThreadPoolExecutor pool = ApplyThreadPool.getThreadPoolExector();

	// 读取Oracle中任务信息类
	private OracleJDBC oracleJDBC;
	// 搜索查询类
	private SearchingData search;
	// 持久化类
	private AllInternet allInternet;

	// 查询需要更新缓存信息的任务
	public static final String QUERY_EXECUTED = "select id,gjc,cjsj,jssj,cjzid,sourceid from JHRW_WWC";

	// 查询最近一天内已经完成的任务
	public static final String QUERY_FINISHED = "select id,gjc,cjsj,jssj,cjzid,sourceid from JHRW_YWC";

	// 所有任务
	public static final String ALL_TASKS = "select id,gjc,cjsj,jssj,cjzid,sourceid from JHRW_ALL";

	public TaskUpdate() {
		this.oracleJDBC = new OracleJDBC();
		this.search = new SearchingData();
		this.allInternet = new AllInternet(MybatisConfig.ServerEnum.sentiment);
		// 遇到异常平滑关闭线程池
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				pool.shutdown();
			}
		}));
	}

	public static void main(String[] args) throws SQLException {
		TaskUpdate taskUpdate = new TaskUpdate();
		// 只进行一次
		//		taskUpdate.tackleAllTasks();
		taskUpdate.tackleExecutedTasks();
		taskUpdate.tackleFinishedTasks();

		pool.shutdown();
		while (!pool.isTerminated()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error("Exception: {}", LogbackUtil.expection2Str(e));
			}
		}
	}

	/**
	 * 获取全部任务
	 */
	public void tackleAllTasks() {
		logger.info("Updating all tasks ...");
		tackleTasks(ALL_TASKS);
	}

	/**
	 * 获取需要更新的任务
	 */
	public void tackleExecutedTasks() {
		logger.info("Updating executed tasks ...");
		tackleTasks(QUERY_EXECUTED);
	}

	/**
	 * 获取一天内已经完成的任务
	 */
	public void tackleFinishedTasks() {
		logger.info("Updating finished tasks ...");
		tackleTasks(QUERY_FINISHED);
	}

	private void tackleTasks(String query) {
		HashMap<String, InternetTask> tasks;
		try {
			tasks = getTasks(query);
			if (tasks == null) {
				return;
			}
			logger.info("Updating tasks' size={}", tasks.size());
			for (Entry<String, InternetTask> tmp : tasks.entrySet()) {
				pool.execute(new TaskUpdateRunnable(search, allInternet, tmp.getValue()));
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
		String id = "";
		InternetTask task = null;
		while (rs.next()) {
			if (!id.equals(rs.getString("id"))) {
				if (task != null) {
					if (result.get(task.getIdentify()) == null) {
						result.put(task.getIdentify(), task);
					} else {
						InternetTask tmp = result.get(task.getIdentify());
						tmp.setSource_ids(mergeSourceId(tmp.getSource_ids(), task.getSource_ids()));
						result.put(task.getIdentify(), tmp);
					}
				}
				task = new InternetTask(CheckSumUtils.getMD5(
						rs.getString("id") + rs.getString("cjzid")
								+ TimeUtils.transToSolrDateStr(rs.getTimestamp("cjsj").getTime())).toUpperCase(), //
						rs.getString("gjc"), //
						TimeUtils.transToSolrDateStr(rs.getTimestamp("cjsj").getTime()), //
						(rs.getTimestamp("jssj") == null) ? TimeUtils.transToSolrDateStr(System.currentTimeMillis())
								: TimeUtils.transToSolrDateStr(rs.getTimestamp("jssj").getTime()), //
						rs.getString("sourceid"));
			} else {
				task.setSource_ids(mergeSourceId(task.getSource_ids(), rs.getString("sourceid")));
			}
			id = rs.getString("id");
		}
		if (task == null) {
			return result;
		}
		// 最后一个添加
		if (result.get(task.getIdentify()) == null) {
			result.put(task.getIdentify(), task);
		} else {
			InternetTask tmp = result.get(task.getIdentify());
			tmp.setSource_ids(mergeSourceId(tmp.getSource_ids(), task.getSource_ids()));
			result.put(task.getIdentify(), tmp);
		}

		return result;
	}

	public void close() {
		oracleJDBC.close();
		search.close();
		pool.shutdown();
		try {
			pool.awaitTermination(300, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
			throw new RuntimeException(e);
		}
	}

	private static String mergeSourceId(String ids1, String ids2) {
		Set<String> ids = new HashSet<>();
		ids.addAll(Arrays.asList(ids1.split(",")));
		ids.addAll(Arrays.asList(ids2.split(",")));
		return Joiner.on(",").join(ids.iterator());
	}

}
