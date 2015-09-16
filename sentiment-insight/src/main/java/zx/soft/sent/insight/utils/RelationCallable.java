package zx.soft.sent.insight.utils;

import java.util.concurrent.Callable;

import zx.soft.sent.insight.domain.RelationRequest;

public class RelationCallable<T> implements Callable<T> {

	private RelationRequest request;

	public RelationCallable(RelationRequest request) {
		this.request = request;
	}

	@Override
	public T call() throws Exception {
		return null;
	}

}
