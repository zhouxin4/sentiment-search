package zx.soft.sent.insight.domain;

/**
 * @author donglei
 */
public class BlogResponse {

	// 评论人
	private String ru;
	// 评论时间
	private String rt;
	// 评论内容
	private String rc;

	public String getRu() {
		return ru;
	}

	public String getRt() {
		return rt;
	}

	public String getRc() {
		return rc;
	}

	private BlogResponse(ResponseBuilder builder) {
		this.ru = builder.ru;
		this.rt = builder.rt;
		this.rc = builder.rc;
	}

	/**
	 * The builder class.
	 */
	public static class ResponseBuilder {
		private String ru;
		private String rt;
		private String rc;

		public ResponseBuilder() {
		}

		public ResponseBuilder responseUser(String ru) {
			this.ru = ru;
			return this;
		}

		public ResponseBuilder responseTime(String rt) {
			this.rt = rt;
			return this;
		}

		public ResponseBuilder responseContent(String rc) {
			this.rc = rc;
			return this;
		}

		public BlogResponse build() {
			return new BlogResponse(this);
		}
	}

}
