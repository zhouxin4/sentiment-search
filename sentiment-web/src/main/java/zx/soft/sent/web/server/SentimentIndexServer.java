package zx.soft.sent.web.server;

import java.util.Properties;

import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Server;
import org.restlet.data.Protocol;

import zx.soft.sent.web.application.SentiIndexApplication;
import zx.soft.utils.config.ConfigUtil;
import zx.soft.utils.jackson.ReplaceConvert;

/**
 * 舆情搜索Server
 *
 * 索引示例：
 * http://localhost:8900/sentiment/index   POST
 * 
 * @author wanggang
 */
public class SentimentIndexServer {

	private final Component component;
	private final SentiIndexApplication sentiIndexApplication;

	private final int PORT;

	public SentimentIndexServer() {
		Properties props = ConfigUtil.getProps("web-server.properties");
		PORT = Integer.parseInt(props.getProperty("api.port"));
		component = new Component();
		sentiIndexApplication = new SentiIndexApplication();
	}

	/**
	 * 主函数
	 */
	public static void main(String[] args) {

		SentimentIndexServer server = new SentimentIndexServer();
		server.start();
	}

	public void start() {
		// 需要设置最大线程数和连接数，否则高并发时请求超时
		//		component.getServers().add(Protocol.HTTP, PORT);
		//		component.getContext().getParameters().add("maxThreads", "512");
		//		component.getContext().getParameters().add("minThreads", "100");
		//		component.getContext().getParameters().add("lowThreads", "200");
		//		component.getContext().getParameters().add("maxQueued", "100");
		//		component.getContext().getParameters().add("maxConnectionsPerHost", "128");
		//		component.getContext().getParameters().add("initialConnections", "255");
		//		component.getContext().getParameters().add("maxTotalConnections", "1024");
		//		component.getContext().getParameters().add("maxIoIdleTimeMs", "100");
		//		component.getContext().getParameters().add("connectionManagerTimeout", "1000");
		Context context = new Context();
		context.getParameters().add("maxThreads", "512");
		context.getParameters().add("minThreads", "100");
		context.getParameters().add("lowThreads", "200");
		context.getParameters().add("maxConnectionsPerHost", "128");
		context.getParameters().add("initialConnections", "255");
		context.getParameters().add("maxTotalConnections", "1024");
		context.getParameters().add("maxIoIdleTimeMs", "100");
		context.getParameters().add("connectionManagerTimeout", "1000");
		Server server = new Server(context, Protocol.HTTP, PORT);
		component.getServers().add(server);
		try {
			component.getDefaultHost().attach("/sentiment/index", sentiIndexApplication);
			ReplaceConvert.configureJacksonConverter();
			component.start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void stop() {
		try {
			component.stop();
			sentiIndexApplication.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
