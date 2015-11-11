package zx.soft.sent.insight.domain;

public class FollowDetail {

	// 关注人
	private String followUser;
	// 关注时间
	private String followTime;
	// 关注内容
	private String followContent;
	// 关注类型
	private int followType;
	// 博客ID
	private String id;
	// 被关注人虚拟账号
	private String followVirtual;

	public String getFollowUser() {
		return followUser;
	}

	public String getFollowTime() {
		return followTime;
	}

	public String getFollowContent() {
		return followContent;
	}

	public int getFollowType() {
		return followType;
	}

	public String getId() {
		return id;
	}

	public String getFollowVirtual() {
		return followVirtual;
	}

	private FollowDetail(FollowBuilder builder) {
		this.followUser = builder.followUser;
		this.followTime = builder.followTime;
		this.followContent = builder.followContent;
		this.followType = builder.followType;
		this.id = builder.id;
		this.followVirtual = builder.followVirtual;
	}

	/**
	 * The builder class.
	 */
	public static class FollowBuilder {
		private String followUser;
		private String followTime;
		private String followContent;
		private int followType;
		private String id;
		private String followVirtual;

		public FollowBuilder() {
		}

		public FollowBuilder followUser(String ru) {
			this.followUser = ru;
			return this;
		}

		public FollowBuilder followTime(String rt) {
			this.followTime = rt;
			return this;
		}

		public FollowBuilder followContent(String rc) {
			this.followContent = rc;
			return this;
		}

		public FollowBuilder followType(int ft) {
			this.followType = ft;
			return this;
		}

		public FollowBuilder followId(String id) {
			this.id = id;
			return this;
		}

		public FollowBuilder followVirtual(String fv) {
			this.followVirtual = fv;
			return this;
		}

		public FollowDetail build() {
			return new FollowDetail(this);
		}
	}
}
