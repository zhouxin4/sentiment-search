package zx.soft.sent.web.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.common.SolrDocument;
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
import zx.soft.utils.algo.TopN;
import zx.soft.utils.algo.TopN.KeyValue;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

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
		Map<String, Long> facet = new HashMap<String, Long>();
		long startTime = System.currentTimeMillis();
		QueryResult queryResult = QueryCore.getInstance().queryData(queryParams, true);
		System.out.println("------------" + queryResult.getNumFound());
		if (queryResult.getNumFound() > 100000) {
			queryParams.setFacetField("nickname");
			QueryResult queryResults = QueryCore.getInstance().queryData(queryParams, true);
			for (SimpleFacetInfo info : queryResults.getFacetFields()) {
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
		} else {
			Multiset<String> counts = HashMultiset.create();
			queryParams.setFl("nickname");
			for (int i = 0; i < queryResult.getNumFound(); i += 5000) {
				queryParams.setRows(5000);
				queryParams.setStart(i);
				QueryResult queryResultss = QueryCore.getInstance().queryData(queryParams, true);
				for (SolrDocument doc : queryResultss.getResults()) {
					if (doc.getFieldValue("nickname") != null) {
						counts.add(doc.getFieldValue("nickname").toString());
					}
				}
			}
			List<KeyValue<String, Integer>> top10 = TopN.topNOnValue(counts, 10);
			for (KeyValue<String, Integer> keyValue : top10) {
				facet.put(keyValue.getKey(), (long) keyValue.getValue());
			}
		}
		return facet;
	}

	public void close() {
		QueryCore.getInstance().close();
	}

}
