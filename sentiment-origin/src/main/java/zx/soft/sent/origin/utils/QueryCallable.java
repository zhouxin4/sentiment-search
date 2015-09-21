package zx.soft.sent.origin.utils;

import java.util.concurrent.Callable;

import zx.soft.sent.common.domain.QueryParams;

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