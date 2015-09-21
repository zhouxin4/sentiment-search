package zx.soft.sent.web.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.common.domain.QueryParams;
import zx.soft.sent.solr.domain.QueryResult;
import zx.soft.sent.solr.domain.SimpleFacetInfo;
import zx.soft.sent.solr.query.QueryCore;
import zx.soft.sent.web.resource.NicknameGroupResource;

/**
 * 舆情搜索应用类
 *
 * @author wanggang
 *
 */
public class NicknameGroupApplication extends Application {
	private static Logger logger = LoggerFactory.getLogger(NicknameGroupApplication.class);

	public NicknameGroupApplication() {
	}

	@Override
	public Restlet createInboundRoot() {
		Router router = new Router(getContext());
		router.attach("/search", NicknameGroupResource.class);
		return router;
	}

	public Map<String, Long> queryData(QueryParams queryParams) {
		long startTime = System.currentTimeMillis();
		QueryResult queryResult = QueryCore.getInstance().queryData(queryParams, true);
		Map<String, Long> facet = new HashMap<String, Long>();
		for (SimpleFacetInfo info : queryResult.getFacetFields()) {
			if (info.getName().equals("nickname")) {
				int i = 0;
				for (Entry<String, Long> entrys : info.getValues().entrySet()) {
					String key = entrys.getKey();
					if (facet.containsKey(key)) {
						facet.put(key, facet.get(key) + entrys.getValue());
					} else {
						facet.put(key, entrys.getValue());
					}
					i++;
					if (i == 10) {
						break;
					}
				}
			}
		}
		logger.info("获取热门帐号耗时: {}" + (System.currentTimeMillis() - startTime));
		return facet;
	}

	public void close() {
		QueryCore.getInstance().close();
	}

}
