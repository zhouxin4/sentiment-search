package zx.soft.sent.insight.server;

import java.io.IOException;
import java.util.Properties;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * 接口说明：
 * 发帖情况分析
 * http://192.168.32.16:8912/insights?trueUser=34232a86c69edcf1e86f3caefcbed9d6
 * 倾向分析
 * http://192.168.32.16:8912/insights/trend?trueUser=34232a86c69edcf1e86f3caefcbed9d6&fq=timestamp:[2015-06-29T13:48:02Z%20TO%202015-07-30T13:48:05Z]
 * 关系分析
 * http://192.168.32.16:8912/insights/relation?trueUser=34232a86c69edcf1e86f3caefcbed9d6&fq=timestamp:[2015-06-29T13:48:02Z%20TO%202015-07-30T13:48:05Z]
 * 关联分析
 * http://192.168.32.16:8912/insights/query?trueUser=34232a86c69edcf1e86f3caefcbed9d6&fq=timestamp:[2015-06-29T13:48:02Z%20TO%202015-07-30T13:48:05Z]&type=1
 * 查询接口
 * http://192.168.32.16:8912/insights/query?trueUser=34232a86c69edcf1e86f3caefcbed9d6&fq=timestamp:[2015-06-29T13:48:02Z%20TO%202015-07-30T13:48:05Z]
 *
 * 运行目录：/home/zxdfs/run-work/api/insights
 * 运行命令：cd sentiment-insights
 *        bin/ctl.sh start insightApiServer
 *
 * @author donglei
 */
public class InsightApiServer {

	private static final Logger logger = LoggerFactory.getLogger(InsightApiServer.class);

	// 默认端口
	private static final int DEFAULT_PORT = 8900;
	// Context路径
	private static final String CONTEXT_PATH = "/";
	// Mapping路径
	private static final String MAPPING_URL = "/*";

	/**
	 * 主函数
	 */
	public static void main(String[] args) throws Exception {
		Properties props = new Properties();
		props.load(InsightApiServer.class.getClassLoader().getResourceAsStream("web-server.properties"));
		new InsightApiServer().startJetty(Integer.valueOf(props.getProperty("api.port", String.valueOf(DEFAULT_PORT))));
	}

	private static WebApplicationContext getContext() {
		XmlWebApplicationContext context = new XmlWebApplicationContext();
		return context;
	}

	private static ServletContextHandler getServletContextHandler(WebApplicationContext context) throws IOException {
		ServletContextHandler contextHandler = new ServletContextHandler();
		contextHandler.setErrorHandler(null);
		contextHandler.setContextPath(CONTEXT_PATH);
		contextHandler.addServlet(new ServletHolder(new DispatcherServlet(context)), MAPPING_URL);
		contextHandler.addEventListener(new ContextLoaderListener(context));
		contextHandler.setResourceBase(new ClassPathResource("webapp").getURI().toString());
		return contextHandler;
	}

	private void startJetty(int port) throws Exception {
		logger.debug("Starting server at port {}", port);
		Server server = new Server(port);
		server.setHandler(getServletContextHandler(getContext()));
		server.start();
		logger.info("Server started at port {}", port);
		server.join();
	}

}
