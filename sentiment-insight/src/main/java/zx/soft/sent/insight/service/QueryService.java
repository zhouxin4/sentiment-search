package zx.soft.sent.insight.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import zx.soft.sent.core.domain.QueryParams;
import zx.soft.sent.insight.utils.TrueUserHelper;
import zx.soft.sent.solr.domain.QueryResult;
import zx.soft.sent.solr.insight.UserDomain;
import zx.soft.sent.solr.insight.Virtuals.Virtual;
import zx.soft.sent.solr.query.QueryCore;
import zx.soft.utils.string.ConcatMethod;
import zx.soft.utils.string.StringConcatHelper;

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
		return QueryCore.getInstance().queryData(params, false);
	}

	public Object getRelatedData(QueryParams params, String nickname) {
		List<Virtual> virtuals = TrueUserHelper.getVirtuals(nickname);
		if (virtuals.isEmpty()) {
			logger.info("True user '{}': has no virtuals!", nickname);
			return new QueryResult();
		}
		StringConcatHelper helper = new StringConcatHelper(ConcatMethod.OR);
		for (Virtual virtual : virtuals) {
			helper.add("\"@" + virtual.getNickname() + "\"");
		}
		UserDomain trueUserInfo = TrueUserHelper.getUserInfo(nickname);
		if (trueUserInfo == null) {
			logger.info("True user '{}' does not exist!", nickname);
			return new QueryResult();
		}
		helper.add("\"" + trueUserInfo.getUserName() + "\"");
		params.setFq(params.getFq() + ";content:(" + helper.getString() + ")");
		// 添加关联分析中重点人员昵称标红
		//		if ("*:*".equals(params.getQ())) {
		//			params.setQ("content:(" + helper.getString() + ")");
		//		} else {
		//			params.setQ("(" + params.getQ() + ") AND content:(" + helper.getString() + ")");
		//		}
		return QueryCore.getInstance().queryData(params, false);
	}

}
