package zx.soft.sent.origin.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer.RemoteSolrException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.redis.client.cache.Cache;
import zx.soft.redis.client.cache.RedisCache;
import zx.soft.redis.client.common.Config;
import zx.soft.sent.common.domain.QueryParams;
import zx.soft.sent.common.domain.SentimentConstant;
import zx.soft.utils.config.ConfigUtil;
import zx.soft.utils.log.LogbackUtil;
import zx.soft.utils.regex.RegexUtils;
import zx.soft.utils.string.StringUtils;
import zx.soft.utils.time.TimeUtils;

/**
 * 搜索舆情数据
 *
 * @author wanggang
 *
 */
public class QueryCore {

	private static Logger logger = LoggerFactory.getLogger(QueryCore.class);

	public enum Shards {
		shard1, shard2, shard3, shard4, shard5, shard6
	};

	private final CloudSolrServer cloudServer;

	private final Cache cache;

	private static QueryCore core = new QueryCore();

	private QueryCore() {
		cache = new RedisCache(Config.get("redis.rp.slave"), Integer.parseInt(Config.get("redis.rp.port")),
				Config.get("redis.password"));
		Properties props = ConfigUtil.getProps("solr_params.properties");
		try {
			cloudServer = new CloudSolrServer(props.getProperty("cdh5_zookeeper_cloud"));
			cloudServer.setDefaultCollection(props.getProperty("cache_collection"));
			cloudServer.setZkConnectTimeout(Integer.parseInt(props.getProperty("zookeeper_connect_timeout")));
			cloudServer.setZkClientTimeout(Integer.parseInt(props.getProperty("zookeeper_client_timeout")));
			cloudServer.connect();
		} catch (MalformedURLException e) {
			logger.info(LogbackUtil.expection2Str(e));
			throw new RuntimeException();
		}

	}

	public static QueryCore getInstance() {
		return core;
	}

	/**
	 * 测试函数
	 */
	public static void main(String[] args) {
		QueryCore search = QueryCore.getInstance();
		//		QueryParams queryParams = new QueryParams();
		//		queryParams.setQ("*:*");
		//		queryParams.setFq("id:A565130951A4B3EE4C3794076F16897B");
		//timestamp:[2014-04-22T00:00:00Z TO 2014-04-23T00:00:00Z]
		//		queryParams.setSort("timestamp:desc"); // lasttime:desc
		//		queryParams.setStart(0);
		//		queryParams.setRows(0);
		//		queryParams.setWt("json");
		//		queryParams.setFl(""); // nickname,content
		//		queryParams.setHlfl("title,content");
		//		queryParams.setHlsimple("red");
		//		queryParams.setFacetQuery("");
		//		queryParams.setFacetRange("timestamp");
		//		queryParams.setFacetRangeStart("2015-07-10T00:00:00Z");
		//		queryParams.setFacetRangeEnd("2015-07-13T00:00:00Z");
		//		queryParams.setFacetRangeGap("+1HOUR");
		//		queryParams.setFacetField("source_id");
		//		QueryResponse result = search.queryDataWithoutView(queryParams, true);
		//		List<SolrInputDocument> docs = new ArrayList<>();
		//		for (SolrDocument doc : result.getResults()) {
		//			docs.add(transSolrDocumentToInputDocument(doc));
		//		}
		//		search.addDocToSolr(docs);
		//		List<String> records = new ArrayList<>();
		//		for (SolrDocument doc : result.getResults()) {
		//			records.add(JsonUtils.toJsonWithoutPretty(doc));
		//		}
		//
		//		System.out.println(JsonUtils.toJson(result));
		search.deleteQuery("timestamp:[2015-09-23T00:44:00Z TO *]");
		//		search.deleteQuery("*:*");
		search.close();
	}

	public void addDocToSolr(List<SolrInputDocument> docs) {
		try {
			cloudServer.add(docs);
			cloudServer.commit();
		} catch (RemoteSolrException | SolrServerException | IOException e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
		}
	}

	public static SolrInputDocument transSolrDocumentToInputDocument(SolrDocument doc) {
		SolrInputDocument input = new SolrInputDocument();
		for (String field : doc.getFieldNames()) {
			input.setField(field, doc.getFieldValue(field));
		}
		return input;
	}

	public void deleteQuery(String q) {
		try {
			cloudServer.deleteByQuery(q);
			cloudServer.commit();
		} catch (SolrServerException e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
			e.printStackTrace();
		} catch (IOException e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
			e.printStackTrace();
		}
	}

	public List<Count> queryData(SolrQuery query) {
		QueryResponse queryResponse = null;
		try {
			queryResponse = cloudServer.query(query, METHOD.POST);
			// GET方式的时候所有查询条件都是拼装到url上边的，url过长当然没有响应，必然中断talking了
			//			queryResponse = server.query(query, METHOD.GET);
		} catch (SolrServerException e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
			throw new RuntimeException(e);
		}
		FacetField fields = queryResponse.getFacetField("source_id");
		return fields.getValues();
	}

	public QueryResponse queryDataWithoutView(QueryParams queryParams, boolean isPlatformTrans) {
		SolrQuery query = getSolrQuery(queryParams);
		QueryResponse queryResponse = null;
		try {
			queryResponse = cloudServer.query(query, METHOD.POST);
			// GET方式的时候所有查询条件都是拼装到url上边的，url过长当然没有响应，必然中断talking了
			//			queryResponse = server.query(query, METHOD.GET);
		} catch (SolrServerException e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
			throw new RuntimeException(e);
		}
		if (queryResponse == null) {
			logger.error("no response!");
		}
		return queryResponse;
	}

	/**
	 * 根据多条件查询结果数据
	 */
	public QueryResult queryData(QueryParams queryParams, boolean isPlatformTrans) {
		SolrQuery query = getSolrQuery(queryParams);
		QueryResponse queryResponse = null;
		try {
			queryResponse = cloudServer.query(query, METHOD.POST);
			// GET方式的时候所有查询条件都是拼装到url上边的，url过长当然没有响应，必然中断talking了
			//			queryResponse = server.query(query, METHOD.GET);
		} catch (SolrServerException e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
			throw new RuntimeException(e);
		}
		if (queryResponse == null) {
			logger.error("no response!");
		}

		//		System.out.println(queryResponse.getFacetFields());
		//		System.out.println(JsonUtils.toJson(queryResponse.getResults()));

		QueryResult result = new QueryResult();
		result.setQTime(queryResponse.getQTime());
		// header数据太长，暂时不需要
		//				result.setHeader(queryResponse.getHeader());
		result.setSort(queryResponse.getSortValues());
		//		result.setHighlighting(queryResponse.getHighlighting());
		result.setGroup(queryResponse.getGroupResponse());
		result.setFacetQuery(queryResponse.getFacetQuery());
		//		System.out.println(queryResponse.getFacetFields());
		result.setFacetFields(transFacetField(queryResponse.getFacetFields(), queryParams, isPlatformTrans));
		result.setFacetDates(transFacetField(queryResponse.getFacetDates(), queryParams, isPlatformTrans));
		result.setFacetRanges(queryResponse.getFacetRanges());
		result.setFacetPivot(queryResponse.getFacetPivot());
		result.setNumFound(queryResponse.getResults().getNumFound());
		result.setResults(queryResponse.getResults());
		// 处理时间timestamp、lasttime、first_time、update_time
		// 同时，查询出来的结果比原先的晚8小时，所以需要将得到的时间减少8小时
		tackleTime(result);
		// 将highlight移到result中，减少数据量，同时方便调用
		for (int i = 0; i < result.getResults().size(); i++) {
			if (queryResponse.getHighlighting() != null) {
				for (String hl : queryParams.getHlfl().split(",")) {
					if (queryResponse.getHighlighting().get(result.getResults().get(i).getFieldValue("id")).get(hl) != null) {
						result.getResults()
								.get(i)
								.setField(
										hl,
										queryResponse.getHighlighting()
												.get(result.getResults().get(i).getFieldValue("id")).get(hl).get(0));
					}
				}
			}
		}

		logger.info("numFound=" + queryResponse.getResults().getNumFound());
		logger.info("QTime=" + queryResponse.getQTime());

		return result;
	}

	/**
	 * 查询时间有8小时误差，在这里修正
	 */
	private void tackleTime(QueryResult result) {
		SolrDocument str = null;
		for (int i = 0; i < result.getResults().size(); i++) {
			str = result.getResults().get(i);
			if (str.getFieldValueMap().get("timestamp") != null) {
				result.getResults()
						.get(i)
						.setField(
								"timestamp",
								TimeUtils
										.transStrToCommonDateStr(str.getFieldValueMap().get("timestamp").toString(), 8));
			}
			if (str.getFieldValueMap().get("lasttime") != null) {
				result.getResults()
						.get(i)
						.setField("lasttime",
								TimeUtils.transStrToCommonDateStr(str.getFieldValueMap().get("lasttime").toString(), 8));
			}
			if (str.getFieldValueMap().get("first_time") != null) {
				result.getResults()
						.get(i)
						.setField(
								"first_time",
								TimeUtils.transStrToCommonDateStr(str.getFieldValueMap().get("first_time").toString(),
										8));
			}
			if (str.getFieldValueMap().get("update_time") != null) {
				result.getResults()
						.get(i)
						.setField(
								"update_time",
								TimeUtils.transStrToCommonDateStr(str.getFieldValueMap().get("update_time").toString(),
										8));
			}
		}
	}

	private List<SimpleFacetInfo> transFacetField(List<FacetField> facets, QueryParams queryParams,
			boolean isPlatformTrans) {
		List<SimpleFacetInfo> result = new ArrayList<>();
		if (facets == null) {
			return null;
		}
		String fqPlatform = "";
		for (String str : queryParams.getFq().split(";")) {
			if (str.contains("platform")) {
				fqPlatform = str;
			}
		}
		for (FacetField facet : facets) {
			SimpleFacetInfo sfi = new SimpleFacetInfo();
			sfi.setName(facet.getName());
			HashMap<String, Long> t = new LinkedHashMap<>();
			for (Count temp : facet.getValues()) {
				if ("platform".equalsIgnoreCase(facet.getName())) {
					if (fqPlatform.contains("platform")) {
						if ((fqPlatform.split(":"))[1].trim().contains((temp.getName()))) {
							if (isPlatformTrans) {
								t.put(SentimentConstant.PLATFORM_ARRAY[Integer.parseInt(temp.getName())],
										temp.getCount());
							} else {
								t.put(temp.getName(), temp.getCount());
							}
						}
					} else {
						if (isPlatformTrans) {
							// 目前我们的平台类型共有11个，如果超过11则不处理
							if (Integer.parseInt(temp.getName()) < SentimentConstant.PLATFORM_ARRAY.length) {
								t.put(SentimentConstant.PLATFORM_ARRAY[Integer.parseInt(temp.getName())],
										temp.getCount());
							}
						} else {
							t.put(temp.getName(), temp.getCount());
						}
					}
				} else if ("source_id".equalsIgnoreCase(facet.getName())) {
					if ((t.size() < SentimentConstant.PLATFORM_ARRAY.length) && (temp.getCount() > 0)) {
						t.put(temp.getName() + "," + cache.hget(SentimentConstant.SITE_MAP, temp.getName()),
								temp.getCount());
					} else {
						break;
					}
				} else {
					if (temp.getCount() > 0) {
						t.put(temp.getName(), temp.getCount());
					} else {
						break;
					}
				}
			}
			sfi.setValues(t);
			result.add(sfi);
		}
		return result;
	}

	/**
	 * 组合查询类
	 * @param q=abc或者[1 TO 100]
	 * @param fq=+f1:abc,dec;-f2:cde,fff;...
	 * @param fl=username,nickname,content,...
	 * @param hlfl=title,content,...
	 * @param sort=platform:desc,source_id:asc,...
	 * @param facetQuery={!key="day1"}timestamp:[NOW/MONTH-12MONTH TO NOW/MONTH-6MONTH],{!key="day2"}timestamp:[NOW/MONTH-18MONTH TO NOW/MONTH-12MONTH],...
	 * @param facetField=nickname,platform,source_id,... 默认platform全返回，其他域只返回前10
	 * @return
	 */
	private SolrQuery getSolrQuery(QueryParams queryParams) {

		SolrQuery query = new SolrQuery();
		if (queryParams.getQ() != "") {
			query.setQuery(queryParams.getQ());
		}
		// 忽略版本信息，否则会对分类统计产生影响
		String[] vinfo = null;
		query.add("version", vinfo);
		// 分片失效忽略
		query.set("shards.tolerant", true);
		// 设置关键词连接逻辑，默认是AND
		query.set("q.op", queryParams.getQop());
		if (queryParams.getFq() != "") {
			for (String fq : queryParams.getFq().split(";")) {
				if (fq.isEmpty()) {
					continue;
				}
				if (fq.contains("source_id")) {
					if (transCacheFq(fq) != "") {
						query.addFilterQuery(transCacheFq(fq));
					} else {
						logger.error("fq=" + fq + " is null.");
					}
				} else {
					query.addFilterQuery(transFq(fq));
				}
			}
		}
		if (queryParams.getSort() != "") {
			String sortStr = queryParams.getSort();
			List<String> funcs = RegexUtils.findMatchStrs(sortStr, "\\(.*\\)", true);
			int i = 0;
			for (String func : funcs) {
				sortStr = sortStr.replace(func, "(" + i + ")");
				i++;
			}
			for (String sort : sortStr.split(",")) {
				List<String> parterns = RegexUtils.findMatchStrs(sort, "\\((\\d+)\\)", false);
				if (!parterns.isEmpty()) {
					int tmp = Integer.parseInt(parterns.get(0));
					sort = sort.replaceAll("\\(" + parterns.get(0) + "\\)", funcs.get(tmp));
				}
				query.addSort(sort.split(":")[0], "desc".equalsIgnoreCase(sort.split(":")[1]) ? ORDER.desc : ORDER.asc);
			}
		}
		if (queryParams.getStart() != 0) {
			query.setStart(queryParams.getStart());
		}
		if (queryParams.getRows() != 10) {
			query.setRows(queryParams.getRows());
		}
		if (queryParams.getFl() != "") {
			query.setFields(queryParams.getFl().split(","));
		}
		if (queryParams.getWt() != "") {
			query.set("wt", queryParams.getWt());
		}
		if (queryParams.getHlfl() != "") {
			query.setHighlight(true).setHighlightSnippets(1);
			query.addHighlightField(queryParams.getHlfl());
		}
		if (queryParams.getHlsimple() != "") {
			query.setHighlightSimplePre("<" + queryParams.getHlsimple() + ">");
			query.setHighlightSimplePost("</" + queryParams.getHlsimple() + ">");
		}
		if (queryParams.getFacetQuery() != "") {
			for (String fq : queryParams.getFacetQuery().split(",")) {
				query.addFacetQuery(fq);
			}
		}
		if (queryParams.getFacetField() != "") {
			//			query.setFacet(true);
			//			query.addFacetField(queryParams.getFacetField().split(","));
			for (String field : queryParams.getFacetField().split(",")) {
				query.addFacetField(field);
				query.set("f." + field + ".facet.method", "fcs");
				query.set("f." + field + ".facet.limit", 50);
			}

		}

		// 按日期分类查询
		if (queryParams.getFacetDate().size() == 4) {
			for (Entry<String, String> facetDate : queryParams.getFacetDate().entrySet()) {
				query.set(facetDate.getKey(), facetDate.getValue());
			}
		}

		if (queryParams.getFacetRange() != "") {
			query.setFacet(true);
			query.set("facet.range", queryParams.getFacetRange());
			query.set("f." + queryParams.getFacetRange() + ".facet.range.start", queryParams.getFacetRangeStart());
			query.set("f." + queryParams.getFacetRange() + ".facet.range.end", queryParams.getFacetRangeEnd());
			query.set("f." + queryParams.getFacetRange() + ".facet.range.gap", queryParams.getFacetRangeGap());
		}

		if (queryParams.isShard() && !StringUtils.isEmpty(queryParams.getShardName())) {
			query.set("shards", queryParams.getShardName());
		}

		return query;
	}

	private String transCacheFq(String fqs) {
		if (fqs.contains("AND") || fqs.contains("OR")) {
			return fqs;
		}
		String result = "";
		String sites = fqs.split(":")[1];
		if ((sites.indexOf(",") < 0) && (sites.length() == 32)) {
			sites = cache.hget(SentimentConstant.SITE_GROUPS, sites);
			if (sites == null) {
				return "";
			}
		}
		int count = 0;
		for (String site : sites.split(",")) {
			if (count++ > 250) {
				break;
			}
			result = result + fqs.split(":")[0] + ":" + site + " OR ";
		}
		result = result.substring(0, result.length() - 4);
		if (fqs.contains("-")) {
			result = result.replace("OR", "AND");
		}
		return result;
	}

	public static String transFq(String fqs) {
		if (fqs.contains("AND") || fqs.contains("OR")) {
			return fqs;
		}
		int index = fqs.indexOf(":");
		String result = "";
		for (String str : fqs.substring(index + 1).split(",")) {
			result = result + fqs.substring(0, index) + ":" + str + " OR ";
		}
		result = result.substring(0, result.length() - 4);
		if (fqs.charAt(0) == '-' || fqs.contains(";-")) {
			result = result.replace("OR", "AND");
		}
		return result;
	}

	/**
	 * 获取资源站点和名称列表
	 */
	public Map<String, String> getSourceIdAndNames() {
		return cache.hgetAll(SentimentConstant.SITE_MAP);
	}

	/**
	 * 关闭资源
	 */
	public void close() {
		cloudServer.shutdown();
		cache.close();
	}

}