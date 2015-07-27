package zx.soft.sent.insight.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.common.SolrDocument;

public class UserDocCache {
	private static Map<String, SolrDocCache> docMaps = new HashMap<>();
	private static Map<String, Long> lasttimeMap = new HashMap<>();

	public static void addSolrDoc(String queryMD5, SolrDocument doc) {
		lasttimeMap.put(queryMD5, System.currentTimeMillis());
		SolrDocCache cache = docMaps.get(queryMD5);
		if (cache != null) {
			cache.putSolrDocument(doc);
		} else {
			cache = new SolrDocCache();
			cache.putSolrDocument(doc);
			if (docMaps.size() >= 10) {
				for (Entry<String, SolrDocCache> entry : docMaps.entrySet()) {
					SolrDocCache tmp = entry.getValue();
					try {
						tmp.getSolrDocument(0);
					} catch (Exception e) {
					}
				}
			}

		}

	}

}
