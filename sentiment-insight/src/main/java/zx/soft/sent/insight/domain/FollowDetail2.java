package zx.soft.sent.insight.domain;

import org.apache.solr.common.SolrDocument;

public class FollowDetail2 {
	// 关注人
	private String followUser;
	// 关注时间
	private String followTime;
	// 关注内容
	private String followContent;
	// 关注类型
	private int followType;
	// 博客ID
	private SolrDocument followDoc;
	// 被关注人虚拟账号
	private String followVirtual;

	public String getFollowUser() {
		return followUser;
	}

	public void setFollowUser(String followUser) {
		this.followUser = followUser;
	}

	public String getFollowTime() {
		return followTime;
	}

	public void setFollowTime(String followTime) {
		this.followTime = followTime;
	}

	public String getFollowContent() {
		return followContent;
	}

	public void setFollowContent(String followContent) {
		this.followContent = followContent;
	}

	public int getFollowType() {
		return followType;
	}

	public void setFollowType(int followType) {
		this.followType = followType;
	}

	public SolrDocument getFollowDoc() {
		return followDoc;
	}

	public void setFollowDoc(SolrDocument followDoc) {
		this.followDoc = followDoc;
	}

	public String getFollowVirtual() {
		return followVirtual;
	}

	public void setFollowVirtual(String followVirtual) {
		this.followVirtual = followVirtual;
	}

}
