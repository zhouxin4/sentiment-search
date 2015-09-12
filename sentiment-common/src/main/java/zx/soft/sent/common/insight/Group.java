package zx.soft.sent.common.insight;

import java.util.List;

public class Group {

	private GroupInfo unit;
	private List<KeyWord> keyWords;

	public GroupInfo getUnit() {
		return unit;
	}

	public void setUnit(GroupInfo unit) {
		this.unit = unit;
	}

	public List<KeyWord> getKeyWords() {
		return keyWords;
	}

	public void setKeyWords(List<KeyWord> keyWords) {
		this.keyWords = keyWords;
	}

	public static class GroupInfo {
		private int uid;
		private String value;
		private String description;
		private String trueUserId;
		private int activeCount;
		private String creator;
		private int creatorAreaCode;
		private String lasttime;
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

		public String getTrueUserId() {
			return trueUserId;
		}

		public void setTrueUserId(String trueUserId) {
			this.trueUserId = trueUserId;
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

		public String getLasttime() {
			return lasttime;
		}

		public void setLasttime(String lasttime) {
			this.lasttime = lasttime;
		}

	}

	public static class KeyWord {
		private int uid;
		private int unitId;
		private String value;
		private String lasttime;
		private int page;
		private int size;

		public int getUid() {
			return uid;
		}

		public void setUid(int uid) {
			this.uid = uid;
		}

		public int getUnitId() {
			return unitId;
		}

		public void setUnitId(int unitId) {
			this.unitId = unitId;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getLasttime() {
			return lasttime;
		}

		public void setLasttime(String lasttime) {
			this.lasttime = lasttime;
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

}
