package zx.soft.sent.solr.query;

import org.junit.Assert;
import org.junit.Test;

public class QueryCoreTest {

	@Test
	public void testTransNicknameFq() {
		String content1 = "nickname:\"北风（温云超, Yunchao Wen）\",\"北风（温云超, Yunchao Wen）\"";
		Assert.assertTrue("nickname:\"北风（温云超, Yunchao Wen）\" OR nickname:\"北风（温云超, Yunchao Wen）\"".equals(QueryCore
				.transNicknameFq(content1)));
		content1 = "nickname:\"北风（温云超, Yunchao Wen）\"";
		Assert.assertTrue("nickname:\"北风（温云超, Yunchao Wen）\"".equals(QueryCore.transNicknameFq(content1)));
		content1 = "nickname:北风,温云超";
		Assert.assertTrue("nickname:北风 OR nickname:温云超".equals(QueryCore.transNicknameFq(content1)));
		content1 = "-nickname:\"北风（温云超, Yunchao Wen）\",\"北风（温云超, Yunchao Wen）\"";
		Assert.assertTrue("-nickname:\"北风（温云超, Yunchao Wen）\" AND -nickname:\"北风（温云超, Yunchao Wen）\"".equals(QueryCore
				.transNicknameFq(content1)));
		content1 = "-nickname:\"北风（温云超, Yunchao Wen）\"";
		Assert.assertTrue("-nickname:\"北风（温云超, Yunchao Wen）\"".equals(QueryCore.transNicknameFq(content1)));
		content1 = "-nickname:北风,温云超";
		Assert.assertTrue("-nickname:北风 AND -nickname:温云超".equals(QueryCore.transNicknameFq(content1)));
	}
}
