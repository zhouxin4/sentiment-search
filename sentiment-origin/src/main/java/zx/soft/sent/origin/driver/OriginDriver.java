package zx.soft.sent.origin.driver;

import zx.soft.sent.origin.server.OriginServer;
import zx.soft.utils.driver.ProgramDriver;

/**
 * 驱动类
 *
 * @author donglei
 *
 */
public class OriginDriver {

	/**
	 * 主函数
	 */
	public static void main(String[] args) {

		int exitCode = -1;
		ProgramDriver pgd = new ProgramDriver();
		try {
			// 运行在bigdata1  --  192.168.32.22机器上
			// 目录：/root/run-work/api/origin  端口：8900
			pgd.addClass("originServer", OriginServer.class, "舆情数据索引接口");
			pgd.driver(args);
			// Success
			exitCode = 0;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

		System.exit(exitCode);

	}

}
