package zx.soft.sent.common.domain;

import java.util.HashMap;

/**
 * 查询参数类
 *
 * @author wanggang
 *
 */
public class QueryParams implements Cloneable {

	private String q = "*:*";
	private String fq = "";
	private String sort = "";
	private int start = 0;
	private int rows = 10;
	private String fl = "";
	private String wt = "json";
	private String hlfl = "";
	private String hlsimple = "";
	private String facetQuery = "";
	private String facetField = "";
	// 设置关键词连接逻辑是AND
	private String qop = "AND";
	// 增加按日期分类统计，主要有以下4个参数
	// facet.date，facet.date.start，facet.date.end，facet.date.gap
	// 默认，facet=true
	private HashMap<String, String> facetDate = new HashMap<>();

	// 范围查询通用字段  added by donglei
	private String facetRange = "";
	private String facetRangeStart = "";
	private String facetRangeEnd = "";
	private String facetRangeGap = "";

	// 分片搜索标志 added by donglei
	private String shardName = "";
	private boolean isShard = false;

	public QueryParams() {
		//
	}

	@SuppressWarnings("unchecked")
	@Override
	public QueryParams clone() {
		QueryParams params = null;
		try {
			params = (QueryParams) super.clone();
			params.facetDate = (HashMap<String, String>)this.facetDate.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}

		return params;
	}

	@Override
	public String toString() {
		return "QueryParams:{q=" + q + ",fq=" + fq + ",sort=" + sort + ",start=" + start + ",rows=" + rows + ",fl="
				+ fl + ",wt=" + wt + ",hlfl=" + hlfl + ",hlsimple=" + hlsimple + ",facetQuery=" + facetQuery
				+ ",facetField=" + facetField + ",qop=" + qop + ",facetRange=" + facetRange + ",facetRangeStart="
				+ facetRangeStart + ",facetRangeEnd=" + facetRangeEnd + "}";
	}

	public void setQ(String q) {
		this.q = q;
	}

	public void setFq(String fq) {
		this.fq = fq;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public void setFl(String fl) {
		this.fl = fl;
	}

	public void setWt(String wt) {
		this.wt = wt;
	}

	public void setHlfl(String hlfl) {
		this.hlfl = hlfl;
	}

	public void setFacetQuery(String facetQuery) {
		this.facetQuery = facetQuery;
	}

	public void setFacetField(String facetField) {
		this.facetField = facetField;
	}

	public String getQ() {
		return q;
	}

	public String getFq() {
		return fq;
	}

	public String getSort() {
		return sort;
	}

	public int getStart() {
		return start;
	}

	public int getRows() {
		return rows;
	}

	public String getFl() {
		return fl;
	}

	public String getWt() {
		return wt;
	}

	public String getHlfl() {
		return hlfl;
	}

	public String getFacetQuery() {
		return facetQuery;
	}

	public String getFacetField() {
		return facetField;
	}

	public String getHlsimple() {
		return hlsimple;
	}

	public void setHlsimple(String hlsimple) {
		this.hlsimple = hlsimple;
	}

	public HashMap<String, String> getFacetDate() {
		return facetDate;
	}

	public void setFacetDate(HashMap<String, String> facetDate) {
		this.facetDate = facetDate;
	}

	public String getQop() {
		return qop;
	}

	public void setQop(String qop) {
		this.qop = qop;
	}

	public String getFacetRange() {
		return facetRange;
	}

	public void setFacetRange(String facetRange) {
		this.facetRange = facetRange;
	}

	public String getFacetRangeStart() {
		return facetRangeStart;
	}

	public void setFacetRangeStart(String facetRangeStart) {
		this.facetRangeStart = facetRangeStart;
	}

	public String getFacetRangeEnd() {
		return facetRangeEnd;
	}

	public void setFacetRangeEnd(String facetRangeEnd) {
		this.facetRangeEnd = facetRangeEnd;
	}

	public String getFacetRangeGap() {
		return facetRangeGap;
	}

	public void setFacetRangeGap(String facetRangeGap) {
		this.facetRangeGap = facetRangeGap;
	}

	public boolean isShard() {
		return isShard;
	}

	public void setShard(boolean isShard) {
		this.isShard = isShard;
	}

	public String getShardName() {
		return shardName;
	}

	public void setShardName(String shardName) {
		this.shardName = shardName;
	}


}
