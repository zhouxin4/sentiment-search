package zx.soft.sent.common.insight;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({ "user" })
public class Group {

	private int uid;
	private String value;
	private String description;
	private int activeCount;
	private String creator;
	private int creatorAreaCode;
	private int systemAreaCode;
	private String lasttime;
	private String keywords;
	private int page;
	private int size;

	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getActiveCount() {
		return activeCount;
	}

	public void setActiveCount(int activeCount) {
		this.activeCount = activeCount;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public int getCreatorAreaCode() {
		return creatorAreaCode;
	}

	public void setCreatorAreaCode(int creatorAreaCode) {
		this.creatorAreaCode = creatorAreaCode;
	}

	public int getSystemAreaCode() {
		return systemAreaCode;
	}

	public void setSystemAreaCode(int systemAreaCode) {
		this.systemAreaCode = systemAreaCode;
	}

	public String getLasttime() {
		return lasttime;
	}

	public void setLasttime(String lasttime) {
		this.lasttime = lasttime;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

}
