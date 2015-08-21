package zx.soft.sent.origin.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import zx.soft.sent.dao.insight.RiakInsight;
import zx.soft.sent.solr.origin.OriginPostModel;
import zx.soft.utils.json.JsonNodeUtils;
import zx.soft.utils.json.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 索引服务类
 *
 * @author wanggang
 *
 */
@Service
public class OriginService {

	private static Logger logger = LoggerFactory.getLogger(OriginService.class);

	private static RiakInsight riak = new RiakInsight();

	public Object getCounts(List<String> mds) {
		Map<String, Map<String, Integer>> results = new HashMap<String, Map<String,Integer>>();
		for (String md : mds) {
			Map<String, Integer> counts = new HashMap<>();
			String post = riak.selectHotkeys("origins", md + "_P1");
			if (post != null) {
				JsonNode count = JsonNodeUtils.getJsonNode(post, "count");
				JsonNode origin = JsonNodeUtils.getJsonNode(post, "origin");
				counts.put("count", count.intValue());
				counts.put("origin", origin.intValue());
			}
			results.put(md, counts);
		}
		return results;

	}

	public Object getOriginPosts(String identify, int start, int rows) {
		OriginPostModel posts = new OriginPostModel();
		int sDoc = start + 1;
		int eDoc = start + rows;
		int sPage = sDoc % 10 == 0 ? sDoc / 10 : sDoc / 10 + 1;
		int ePage = eDoc % 10 == 0 ? eDoc / 10 : eDoc / 10 + 1;
		int docNum = 0;
		for (int p = sPage; p <= ePage; p++) {
			String post = riak.selectHotkeys("origins", identify + "_P" + p);
			if (post == null) {
				break;
			}
			OriginPostModel tmp = JsonUtils.getObject(post, OriginPostModel.class);
			int i = 1;
			if (p == sPage) {
				i = sDoc % 10;
				posts.setCount(tmp.getCount());
				posts.setOrigin(tmp.getOrigin());
				posts.setPage(tmp.getPage());
			}
			while (docNum != rows && i <= tmp.getDocs().size()) {
				posts.addDoc(tmp.getDocs().get(i - 1));
				i++;
				docNum++;
			}

		}
		return posts;
	}



}