package zx.soft.sent.insight.service;

import java.util.Collection;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import zx.soft.sent.common.domain.QueryParams;
import zx.soft.sent.common.insight.TrueUserHelper;
import zx.soft.sent.common.insight.Virtuals.Virtual;
import zx.soft.sent.solr.domain.QueryResult;
import zx.soft.sent.solr.query.QueryCore;
import zx.soft.utils.string.ConcatMethod;
import zx.soft.utils.string.StringConcatHelper;

import com.google.common.collect.Collections2;

/**
 * @author donglei
 */
@Service
public class QueryService {
	private static Logger logger = LoggerFactory.getLogger(QueryService.class);

	public QueryResult queryData(QueryParams params, String nickname) {
		List<Virtual> virtuals = TrueUserHelper.getVirtuals(nickname);
		if (virtuals.isEmpty()) {
			return new QueryResult();
		}
		StringConcatHelper helper = new StringConcatHelper(ConcatMethod.OR);
		for (Virtual virtual : virtuals) {
			helper.add("(nickname:\"" + virtual.getNickname() + "\" AND source_id:" + virtual.getSource_id() + ")");
		}
		params.setFq(params.getFq() + ";" + helper.getString());
		logger.info(params.toString());
		return QueryCore.getInstance().queryData(params, false);
	}

	public Object getRelatedData(QueryParams params, String nickname) {
		Collection<Virtual> virtuals = TrueUserHelper.getVirtuals(nickname);
		if (virtuals.isEmpty()) {
			logger.info("True user '{}': has no virtuals!", nickname);
			return new QueryResult();
		}
		virtuals = Collections2.filter(virtuals, new TrueUserHelper.VirtualPredicate(params.getFq()));
		StringConcatHelper helper = new StringConcatHelper(ConcatMethod.OR);
		for (Virtual virtual : virtuals) {
			//			helper.add("\"@" + virtual.getNickname() + "\"");
			helper.add("(content:\"" + virtual.getNickname() + "\" AND source_id:" + virtual.getSource_id() + ")");
		}
		//		UserDomain trueUserInfo = TrueUserHelper.getUserInfo(nickname);
		//		if (trueUserInfo == null) {
		//			logger.info("True user '{}' does not exist!", nickname);
		//			return new QueryResult();
		//		}
		//		helper.add("\"" + trueUserInfo.getUserName() + "\"");
		//		params.setFq(params.getFq() + ";content:(" + helper.getString() + ")");
		params.setFq(params.getFq() + ";" + helper.getString());
		logger.info(params.toString());
		QueryResult result = QueryCore.getInstance().queryData(params, false);
		for (SolrDocument doc : result.getResults()) {
			for (Virtual virtual : virtuals) {
				if (virtual.getSource_id() == Integer.parseInt(doc.getFieldValue("source_id").toString())
						&& doc.getFieldValue("content").toString().contains(virtual.getNickname())) {
					int len = doc.getFieldValue("content").toString().length();
					int f_i = doc.getFieldValue("content").toString().indexOf(virtual.getNickname());
					int l_i = f_i + virtual.getNickname().length();

					f_i = f_i - 80 < 0 ? 0 : f_i - 80;
					l_i = l_i + 80 > len ? len : l_i + 80;
					doc.setField("content", doc.getFieldValue("content").toString().substring(f_i, l_i));
					doc.setField(
							"content",
							doc.getFieldValue("content").toString()
									.replaceAll(virtual.getNickname(), "<red>" + virtual.getNickname() + "</red>"));
					if (doc.getFieldValue("hit_v") != null) {
						doc.setField("hit_v", doc.getFieldValue("hit_v").toString() + "①" + virtual.getNickname());
					} else {
						doc.setField("hit_v", virtual.getNickname());
					}

				}
			}
		}
		return result;
	}

}
