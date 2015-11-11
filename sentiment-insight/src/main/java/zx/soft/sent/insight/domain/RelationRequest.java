package zx.soft.sent.insight.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import zx.soft.utils.json.JsonUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

/**
 * 关系分析请求
 * @author donglei
 */
public class RelationRequest {
	public enum EndPoint {
		RELATION, POST, DETAIL, FOLLOWS
	};

	// 关系分析请求
	private EndPoint service;
	// 	时间段
	private String timestamp;
	//  真实用户ID
	private String trueUserId;
	//  虚拟帐户列表
	private List<String> virtuals = new ArrayList<>();
	//  平台类型
	private int platform = -1;
	//  网站ID
	private int source_id = -1;
	//  查询关键词
	private String q;
	//  solr系统存储ID
	private String solr_id;
	//  单页显示条数
	private int rows = 10;
	//  分页起始
	private int start = 0;

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(RelationRequest.class).add("service", service).add("trueUser", trueUserId)
				.add("timestamp", timestamp).add("platform", platform).add("source_id", source_id).add("q", q)
				.add("solr_id", solr_id).add("virtuals", virtuals).toString();
	}

	public static void main(String[] args) {
		RelationRequest request = new RelationRequest();
		request.setService(EndPoint.POST);
		request.setPlatform(3);
		request.setTrueUserId("01012f62a89d5f8a6be12fb8595a2832");
		request.setQ("123");
		request.setSource_id(7);
		List<String> virtuals = new ArrayList<>();
		virtuals.add("全球眼光");
		virtuals.add("forwardslash");
		virtuals.add("花小仙女");
		virtuals.add("村口胡大爷哇");
		virtuals.add("-東墻");
		virtuals.add("了了Miracle");
		virtuals.add("钟国仁是初声0000");
		request.setVirtuals(virtuals);
		request.setRows(10);
		request.setStart(0);
		String str = JsonUtils.toJsonWithoutPretty(request);
		System.out.println(str);
		str = "{\"service\":\"POST\",\"trueUserId\":\"01012f62a89d5f8a6be12fb8595a2832\",\"virtuals\":[\"全球眼光\",\"forwardslash\",\"花小仙女\",\"村口胡大爷哇\",\"-東墻\",\"了了Miracle\",\"钟国仁是初声0000\"],\"platform\":3,\"source_id\":7,\"rows\":10,\"start\":0}";
		RelationRequest request2 = JsonUtils.getObject(str, RelationRequest.class);
		System.out.println(JsonUtils.toJsonWithoutPretty(request2));
		System.out.println(JsonUtils.toJsonWithoutPretty(new HashMap<>()));
	}

	public EndPoint getService() {
		return service;
	}

	public void setService(EndPoint service) {
		this.service = service;
	}

	@JsonProperty
	public String getTimestamp() {
		return timestamp;
	}

	@JsonIgnore
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	@JsonProperty
	public String getTrueUserId() {
		return trueUserId;
	}

	@JsonIgnore
	public void setTrueUserId(String trueUserId) {
		this.trueUserId = trueUserId;
	}

	@JsonProperty
	public List<String> getVirtuals() {
		return virtuals;
	}

	@JsonIgnore
	public void setVirtuals(List<String> virtuals) {
		this.virtuals = virtuals;
	}

	@JsonProperty
	public int getPlatform() {
		return platform;
	}

	@JsonIgnore
	public void setPlatform(int platform) {
		this.platform = platform;
	}

	@JsonProperty
	public int getSource_id() {
		return source_id;
	}

	@JsonIgnore
	public void setSource_id(int source_id) {
		this.source_id = source_id;
	}

	@JsonProperty
	public String getQ() {
		return q;
	}

	@JsonIgnore
	public void setQ(String q) {
		this.q = q;
	}

	@JsonProperty
	public String getSolr_id() {
		return solr_id;
	}

	@JsonIgnore
	public void setSolr_id(String solr_id) {
		this.solr_id = solr_id;
	}

	@JsonProperty
	public int getRows() {
		return rows;
	}

	@JsonIgnore
	public void setRows(int rows) {
		this.rows = rows;
	}

	@JsonProperty
	public int getStart() {
		return start;
	}

	@JsonIgnore
	public void setStart(int start) {
		this.start = start;
	}
}
