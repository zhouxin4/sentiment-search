package zx.soft.sent.insight.utils;

import java.util.concurrent.Callable;

import zx.soft.sent.core.domain.QueryParams;
import zx.soft.sent.solr.domain.QueryResult;
import zx.soft.sent.solr.query.QueryCore;

public class QueryCallable implements Callable<QueryResult> {
	private String tag = null;
	private QueryParams params = null;

	public QueryCallable(String tag, QueryParams params) {
		this.tag = tag;
		this.params = params;
	}

	@Override
	public QueryResult call() throws Exception {
		QueryResult result = QueryCore.getInstance().queryData(params, false);
		result.setTag(tag);
		return result;
	}

}