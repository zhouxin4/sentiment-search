package zx.soft.sent.insight.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.client.solrj.response.RangeFacet.Count;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import zx.soft.sent.insight.domain.CommonRequest;
import zx.soft.sent.insight.domain.CommonRequest.UserInfo;
import zx.soft.sent.insight.domain.Group;
import zx.soft.sent.insight.domain.Group.KeyWord;
import zx.soft.sent.insight.domain.PostsResult;
import zx.soft.sent.insight.domain.RawType;
import zx.soft.sent.insight.domain.TrendResult;
import zx.soft.sent.insight.domain.UserDomain;
import zx.soft.sent.insight.domain.Virtuals;
import zx.soft.sent.insight.domain.Virtuals.Virtual;
import zx.soft.sent.insight.utils.VirtualsCache;
import zx.soft.sent.solr.domain.QueryParams;
import zx.soft.sent.solr.domain.QueryResult;
import zx.soft.sent.solr.domain.SimpleFacetInfo;
import zx.soft.sent.solr.query.QueryCore;
import zx.soft.sent.solr.query.QueryCore.Shards;
import zx.soft.utils.http.HttpClientDaoImpl;
import zx.soft.utils.json.JsonNodeUtils;
import zx.soft.utils.json.JsonUtils;
import zx.soft.utils.threads.AwesomeThreadPool;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.primitives.Longs;

/**
 * @author donglei
 */
@Service
public class InsightService {

	private static Logger logger = LoggerFactory.getLogger(InsightService.class);
	private static final String VIRTUAL_URL = "http://192.168.6.120:8080/keyusers/virtualUser";
	private static final String GROUP_URL = "http://192.168.6.120:8080/keyusers/keyword";
	private static final String TRUE_USER = "http://192.168.6.120:8080/keyusers/trueUser/";

	private static QueryCore core = null;
	static {
		core = new QueryCore();
	}

	private static Map<String, String> cates = new HashMap<String, String>();
	static {
		cates.put("民主思潮", "专制独裁民主民主制选举立法政协党派多党合作上访贪官");
		cates.put("食品安全", "食品路边小吃死猪肉麻辣烫地沟油福尔马林细菌寄生虫硫酸亚铁病菌僵尸肉");
		cates.put("敌对势力", "敌对势力藏独达赖分裂势力台独恐怖分子");
		cates.put("利益垄断", "利益财富资源资金 AND 垄断把持操控独占独揽");
		cates.put("涉及消防", "消防燃气液化气酒精高温家用电器  AND  火灾爆炸灭火器失火泄漏");
		cates.put("涉及民生", "民生医药费食品安全公共设施");
		cates.put("周边国家", "韩国日本菲律宾马来西亚文莱印度尼西亚新加坡越南朝鲜俄罗斯蒙古哈萨克斯坦吉尔吉斯塔吉克斯坦阿富汗巴基斯坦尼泊尔不丹缅甸老挝");
	}

	private Map<String, Long> getRelatedNickname(QueryParams params, String nickname) {
		long start = System.currentTimeMillis();
		List<Virtual> virtuals = getVirtuals(nickname);
		RawType util = new RawType();
		for (Virtual virtual : virtuals) {
			util.addQueryParam("fq", "\"" + virtual.getNickname() + "\"");
		}
		params.setFq(params.getFq() + ";" + "content:(" + util.getQueryParams().get("fq") + ")");
		params.setFacetField("nickname");
		params.setShard(true);
		List<Callable<QueryResult>> calls = new ArrayList<>();
		for (Shards shard : Shards.values()) {
			final QueryParams tmp = params.clone();
			tmp.setShardName(shard.name());
			calls.add(new MyCallable(shard.name(), tmp));
		}
		List<QueryResult> results = AwesomeThreadPool.runCallables(6, calls, QueryResult.class);

		for (QueryResult result : results) {
			for (SimpleFacetInfo facetInfo : result.getFacetFields()) {
				if ("nickname".equals(facetInfo.getName())) {
					for (Entry<String, Long> count : facetInfo.getValues().entrySet()) {
						util.countData(count.getKey(), count.getValue());
					}
				}
			}
		}
		for (Virtual vir : virtuals) {
			util.removeData(vir.getNickname());
		}
		logger.info("关系分析耗时: {}", System.currentTimeMillis() - start);
		return util.getDatas();
	}

	public Object relationAnalysed(QueryParams params, String nickname) {
		QueryParams tmp = params.clone();
		//		List<Virtual> virtuals = getVirtuals(nickname);
		//		RawType util = new RawType();
		Map<String, Long> count = getRelatedNickname(tmp, nickname);
		//      暂时只考虑其他账号和真实用户的关系
		//		for (Virtual virtual : virtuals) {
		//			util.addQueryParam("fq", "(nickname:" + virtual.getNickname() + " AND source_id:" + virtual.getSource_id()
		//					+ ")");
		//		}
		//		params.setFq(params.getFq() + ";" + util.getQueryParams().get("fq"));
		//		List<Callable<QueryResult>> calls = new ArrayList<>();
		//		for (String rel : count.keySet()) {
		//			QueryParams queryTmp = params.clone();
		//			queryTmp.setQ(queryTmp.getQ() + " AND " + "\"" + rel + "\"");
		//			calls.add(new MyCallable(rel, queryTmp));
		//		}
		//		List<QueryResult> queryResults = AwesomeThreadPool.runCallables(5, calls, QueryResult.class);
		//		for (QueryResult queryResult : queryResults) {
		//			if (count.containsKey(queryResult.getTag())) {
		//				count.put(queryResult.getTag(), queryResult.getNumFound() + count.get(queryResult.getTag()));
		//			} else {
		//				count.put(queryResult.getTag(), queryResult.getNumFound());
		//			}
		//		}
		List<Entry<String, Long>> hotRels = new ArrayList<>();
		for (Entry<String, Long> entry : count.entrySet()) {
			hotRels.add(entry);
		}
		Collections.sort(hotRels, new Comparator<Entry<String, Long>>() {

			@Override
			public int compare(Entry<String, Long> o1, Entry<String, Long> o2) {
				return -Longs.compare(o1.getValue(), o2.getValue());
			}
		});
		if (hotRels.size() < 10) {
			return hotRels;
		}

		return hotRels.subList(0, 10);
	}

	public Object queryData(QueryParams params, String nickname) {
		List<Virtual> virtuals = getVirtuals(nickname);
		RawType fq = new RawType();
		for (Virtual virtual : virtuals) {
			fq.addQueryParam("fq", "(nickname:" + virtual.getNickname() + " AND source_id:" + virtual.getSource_id()
					+ ")");
		}
		params.setFq(params.getFq() + ";" + fq.getQueryParams().get("fq"));
		return core.queryData(params, false);
	}

	public Object getRelatedData(QueryParams params, String nickname) {
		List<Virtual> virtuals = getVirtuals(nickname);
		RawType postsResult = new RawType();
		for (Virtual virtual : virtuals) {
			postsResult.addQueryParam("fq", "\"" + virtual.getNickname() + "\"");
		}
		UserDomain trueUserInfo = getUserInfo(nickname);
		if(trueUserInfo == null) {
			return new QueryResult();
		}
		postsResult.addQueryParam("fq", "\"" + trueUserInfo.getUserName() + "\"");
		params.setFq(params.getFq() + ";content:(" + postsResult.getQueryParams().get("fq") + ")");
		return core.queryData(params, false);
	}

	public Object getTrendInfos(QueryParams params, String nickname) {
		long start = System.currentTimeMillis();
		List<Virtual> virtuals = getVirtuals(nickname);
		RawType postsResult = new RawType();
		for (Virtual virtual : virtuals) {
			postsResult.addQueryParam("fq",
					"(nickname:" + virtual.getNickname() + " AND source_id:" + virtual.getSource_id() + ")");
		}
		params.setFq(params.getFq() + ";" + postsResult.getQueryParams().get("fq"));
		List<Callable<QueryResult>> calls = new ArrayList<>();
		List<Group> groups = getTrendGroup(nickname);
		for (Group group : groups) {
			for (KeyWord word : group.getKeyWords()) {
				final String cate = group.getUnit().getValue() + "##" + word.getValue();
				final String query = word.getValue();
				final QueryParams tmp = params.clone();
				tmp.setQ(query);
				calls.add(new MyCallable(cate, tmp));
			}
		}
		int numThread = calls.size() / 2 < 10 ? 10 : calls.size() / 2;
		List<QueryResult> queryResults = AwesomeThreadPool.runCallables(numThread, calls, QueryResult.class);

		Map<String, Long> maps = new HashMap<>();
		for (QueryResult result : queryResults) {
			maps.put(result.getTag(), result.getNumFound());
		}
		TrendResult result = new TrendResult();
		for (Entry<String, Long> entry : maps.entrySet()) {
			result.addItem(entry.getKey(), entry.getValue());
		}
		result.sortHotKeys();
		logger.info("获得倾向信息耗时: {}ms", System.currentTimeMillis() - start);

		return result;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object getNicknamePostInfos(QueryParams params, String nickname) {
		long start = System.currentTimeMillis();
		PostsResult postsResults = new PostsResult();
		List<Virtual> virtuals = getVirtuals(nickname);
		if (virtuals.isEmpty()) {
			logger.info("True user '{}': has no virtuals!", nickname);
			return postsResults;
		}
		List<Callable<QueryResult>> calls = new ArrayList<>();
		String fq = params.getFq();
		for (final Virtual virtual : virtuals) {
			final QueryParams tmp = params.clone();
			if (tmp == null) {
				logger.error("Error throwed when Object cloned");
				continue;
			}
			tmp.setFq(fq + ";nickname:" + virtual.getNickname() + ";source_id:" + virtual.getSource_id());
			calls.add(new MyCallable(virtual.getSource_name(), tmp));
		}
		int numThread = calls.size() < 10 ? calls.size() : 10;
		List<QueryResult> queryResults = AwesomeThreadPool.runCallables(numThread, calls,
				QueryResult.class);
		for (QueryResult result : queryResults) {
			for (RangeFacet facet : result.getFacetRanges()) {
				if (facet.getName().equals(params.getFacetRange())) {
					List<Count> counts = facet.getCounts();
					for (Count count : counts) {
						postsResults.addIterm(result.getTag(), count.getValue(), count.getCount());
					}
				}
			}
		}
		logger.info("统计发帖情况耗时: {}ms", System.currentTimeMillis() - start);
		return postsResults;
	}

	public List<Virtual> getVirtuals(String trueUser) {
		Virtuals virtuals = VirtualsCache.getVirtuals(trueUser);
		if (virtuals != null && !virtuals.getVirtuals().isEmpty()) {
			return virtuals.getVirtuals();
		}
		String data = "{\"trueUserId\":\"" + trueUser + "\",\"page\":1,\"size\":100}";
		HttpClientDaoImpl httpclient = new HttpClientDaoImpl();
		long start = System.currentTimeMillis();
		String response = httpclient.doPostAndGetResponse(VIRTUAL_URL, data);
		logger.info("获取虚拟账号耗时: {}", System.currentTimeMillis() - start);
		if (!"error".equals(response)) {
			JsonNode node = JsonNodeUtils.getJsonNode(response, "response");
			List<Virtual> virs = JsonUtils.parseJsonArray(node.toString(), Virtual.class);
			if (!virs.isEmpty()) {
				VirtualsCache.addVirtuals(trueUser, virs);
			}
			return virs;
		}
		return new ArrayList<>();
	}

	public List<Group> getTrendGroup(String trueUser) {
		CommonRequest request = new CommonRequest();
		request.setType("unitword");
		request.setOperation(3);
		UserInfo unit = new UserInfo();
		unit.setCreator("201");
		unit.setCreatorAreaCode(340000);
		unit.setTrueUserId(trueUser);
		unit.setPage(1);
		unit.setSize(20);
		request.setInfo(unit);
		HttpClientDaoImpl httpclient = new HttpClientDaoImpl();
		long start = System.currentTimeMillis();
		String response = httpclient.doPostAndGetResponse(GROUP_URL, JsonUtils.toJsonWithoutPretty(request));
		logger.info("获取关键词分组耗时: {}", System.currentTimeMillis() - start);
		if (!"error".equals(response)) {
			JsonNode node = JsonNodeUtils.getJsonNode(response, "response");
			List<Group> group = JsonUtils.parseJsonArray(node.toString(), Group.class);
			return group;
		}
		return null;
	}

	public UserDomain getUserInfo(String trueUserId) {
		Virtuals virtuals = VirtualsCache.getVirtuals(trueUserId);
		if (virtuals != null && virtuals.getTrueUser() != null) {
			return virtuals.getTrueUser();
		}
		HttpClientDaoImpl httpClient = new HttpClientDaoImpl();
		String response = httpClient.doGet(TRUE_USER + trueUserId);
		JsonNode node = JsonNodeUtils.getJsonNode(response, "response");
		List<UserDomain> users = JsonUtils.parseJsonArray(node.toString(), UserDomain.class);
		if (users.isEmpty()) {
			return null;
		} else {
			VirtualsCache.addTrueUser(trueUserId, users.get(0));
			return users.get(0);
		}
	}

	public static class MyCallable implements Callable<QueryResult> {
		private String tag = null;
		private QueryParams params = null;

		public MyCallable(String tag, QueryParams params) {
			this.tag = tag;
			this.params = params;
		}

		@Override
		public QueryResult call() throws Exception {
			QueryResult result = core.queryData(params, false);
			result.setTag(tag);
			return result;
		}

	}

}