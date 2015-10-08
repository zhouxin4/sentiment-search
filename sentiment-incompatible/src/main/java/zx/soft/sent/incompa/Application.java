package zx.soft.sent.incompa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 全网搜索任务联合接口服务：hefei02
 * 示例：http://192.168.32.12:10000/virtual/7
 *
 * 运行目录：/home/zxdfs/run-work/api/incompatible
 * 运行命令：cd sentiment-incompatible
 *        java -jar sentiment-incompatible-2.2.0.jar &
 * @author donglei
 *
 */
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
