package zx.soft.sent.insight.driver;

import zx.soft.sent.insight.server.InsightApiServer;
import zx.soft.utils.driver.ProgramDriver;

/**
 * 驱动类
 *
 * @author donglei
 *
 */
public class InsightDriver {

	/**
	 * 主函数
	 */
	public static void main(String[] args) {

		int exitCode = -1;
		ProgramDriver pgd = new ProgramDriver();
		try {
			// 运行在192.168.31.26机器上
			// 测试环境： 目录：/home/solr/run-work/api/weibo  端口：8903
			pgd.addClass("insightApiServer", InsightApiServer.class, "重点人员发帖趋势接口");
			pgd.driver(args);
			// Success
			exitCode = 0;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

		System.exit(exitCode);

	}

}
