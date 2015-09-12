package zx.soft.sent.common.insight;

import com.fasterxml.jackson.annotation.JsonProperty;


public class CommonRequest {
	private String type;
	private int operation;
	@JsonProperty("unit")
	private UserInfo info;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getOperation() {
		return operation;
	}

	public void setOperation(int operation) {
		this.operation = operation;
	}

	public UserInfo getInfo() {
		return info;
	}

	public void setInfo(UserInfo info) {
		this.info = info;
	}


	public static class UserInfo {

		private String trueUserId;
		private String creator;
		private int creatorAreaCode;
		private int page;
		private int size;
		public String getTrueUserId() {
			return trueUserId;
		}
		public void setTrueUserId(String trueUserId) {
			this.trueUserId = trueUserId;
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

	}

}
