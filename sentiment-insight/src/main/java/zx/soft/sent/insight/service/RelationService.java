package zx.soft.sent.insight.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import zx.soft.sent.core.domain.QueryParams;
import zx.soft.sent.insight.utils.QueryCallable;
import zx.soft.sent.insight.utils.TrueUserHelper;
import zx.soft.sent.solr.domain.QueryResult;
import zx.soft.sent.solr.domain.SimpleFacetInfo;
import zx.soft.sent.solr.insight.RawType;
import zx.soft.sent.solr.insight.Virtuals.Virtual;
import zx.soft.sent.solr.query.QueryCore.Shards;
import zx.soft.utils.string.ConcatMethod;
import zx.soft.utils.string.StringConcatHelper;
import zx.soft.utils.threads.AwesomeThreadPool;

import com.google.common.collect.Collections2;
import com.google.common.primitives.Longs;

/**
 * @author donglei
 */
@Service
public class RelationService {
	private static Logger logger = LoggerFactory.getLogger(PostService.class);

	private Map<String, Long> getRelatedNickname(QueryParams params, String nickname) {
		long start = System.currentTimeMillis();
		Collection<Virtual> virtuals = TrueUserHelper.getVirtuals(nickname);
		virtuals = Collections2.filter(virtuals, new TrueUserHelper.VirtualPredicate(params.getFq()));
		StringConcatHelper helper = new StringConcatHelper(ConcatMethod.OR);
		for (Virtual virtual : virtuals) {
			helper.add("\"" + virtual.getNickname() + "\"");
		}
		params.setFq(params.getFq() + ";" + "content:(" + helper.getString() + ")");
		params.setFacetField("nickname");
		params.setShard(true);
		List<Callable<QueryResult>> calls = new ArrayList<>();
		for (Shards shard : Shards.values()) {
			final QueryParams tmp = params.clone();
			tmp.setShardName(shard.name());
			calls.add(new QueryCallable(shard.name(), tmp));
		}
		List<QueryResult> results = AwesomeThreadPool.runCallables(6, calls);

		RawType util = new RawType();
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
}
