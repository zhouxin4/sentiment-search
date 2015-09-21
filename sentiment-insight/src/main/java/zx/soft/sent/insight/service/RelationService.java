package zx.soft.sent.insight.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import zx.soft.sent.common.domain.QueryParams;
import zx.soft.sent.common.insight.TrueUserHelper;
import zx.soft.sent.common.insight.Virtuals.Virtual;
import zx.soft.sent.insight.utils.QueryCallable;
import zx.soft.sent.solr.domain.QueryResult;
import zx.soft.sent.solr.domain.SimpleFacetInfo;
import zx.soft.sent.solr.insight.RawType;
import zx.soft.sent.solr.query.QueryCore.Shards;
import zx.soft.utils.string.ConcatMethod;
import zx.soft.utils.string.StringConcatHelper;
import zx.soft.utils.threads.AwesomeThreadPool;

import com.google.common.collect.Collections2;
import com.google.common.primitives.Longs;

/**
 * 关联分析模块
 * @author donglei
 */
@Service
public class RelationService {

	private static Logger logger = LoggerFactory.getLogger(PostService.class);

	/**
	 * 通过对所有虚拟帐号名进行搜索
	 * @param params
	 * @param nickname
	 * @return
	 */
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

	/**
	 * 通过对单个帐号单独facet，并汇总统计－－ 准确度更高，但加重了服务器端facet的压力
	 * @param params
	 * @param nickname
	 * @return
	 */
	private Map<String, Long> getRelatedNicknameV2(QueryParams params, String nickname) {
		long start = System.currentTimeMillis();
		Collection<Virtual> virtuals = TrueUserHelper.getVirtuals(nickname);
		virtuals = Collections2.filter(virtuals, new TrueUserHelper.VirtualPredicate(params.getFq()));
		StringConcatHelper helper = new StringConcatHelper(ConcatMethod.OR);
		for (Virtual virtual : virtuals) {
			helper.add(virtual.getSource_id(), "\"" + virtual.getNickname() + "\"");
		}
		params.setFacetField("nickname");
		List<Callable<QueryResult>> calls = new ArrayList<>();
		Map<Object, String> tmps = helper.getALLString();
		for (Map.Entry<Object, String> entry : tmps.entrySet()) {
			final QueryParams tmp = params.clone();
			tmp.setFq(tmp.getFq() + ";" + "content:(" + entry.getValue() + ")");
			tmp.setFq(tmp.getFq() + ";" + "source_id:" + entry.getKey());
			calls.add(new QueryCallable(entry.getKey().toString(), tmp));
		}
		if (calls.isEmpty()) {
			logger.info("True user '{}': has no virtuals on {}!", nickname, params.getFq());
			return new HashMap<String, Long>();
		}
		List<QueryResult> results = AwesomeThreadPool.runCallables(calls.size(), calls);

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
		Map<String, Long> count = getRelatedNicknameV2(tmp, nickname);
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
