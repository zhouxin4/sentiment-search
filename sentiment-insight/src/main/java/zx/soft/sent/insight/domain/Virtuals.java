package zx.soft.sent.insight.domain;

import java.util.ArrayList;
import java.util.List;

import zx.soft.utils.json.JsonUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class Virtuals implements Cloneable {
	private long timestamp;
	private UserDomain trueUser;
	private List<Virtual> virtuals = new ArrayList<Virtual>();

	public Virtuals(long timestamp, List<Virtual> virtuals) {
		this.timestamp = timestamp;
		this.virtuals = virtuals;
	}

	public Virtuals(long timestamp, UserDomain user) {
		this.timestamp = timestamp;
		this.trueUser = user;
	}

	public Virtuals(long timestamp, UserDomain user, List<Virtual> virtuals) {
		this.timestamp = timestamp;
		this.trueUser = user;
		this.virtuals = virtuals;

	}

	public static void main(String[] args) {
		Virtual vir = new Virtual();
		vir.setId(0);
		vir.setTrueUser("159b6a505277b6f39655cbfcb83a84b4");
		vir.setPlatform(3);
		vir.setNickname("阿什顿");
		vir.setSource_id(8);
		vir.setSource_name("腾讯微博");
		vir.setLasttime("2015-07-15 10:04:04.0");
		System.out.println(JsonUtils.toJson(vir));
		System.out
				.println(JsonUtils
						.getObject(
								"{\"virtualId\":7,\"trueUserId\":\"159b6a505277b6f39655cbfcb83a84b4\",\"virtualUserAccount\":\"阿什顿\",\"webType\":\"3\",\"webName\":\"腾讯微博\",\"webSourceId\":8,\"lasttime\":\"2015-07-15 10:04:04.0\"}",
								Virtual.class));
	}

	@Override
	public Virtuals clone() {
		Virtuals user = null;
		try {
			user = (Virtuals) super.clone();
			if (this.trueUser != null) {
				user.trueUser = this.trueUser.clone();
			}
			List<Virtual> virs = new ArrayList<>();
			for(Virtual vir : user.getVirtuals()) {
				virs.add(vir.clone());
			}
			user.setVirtuals(virs);
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return user;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public UserDomain getTrueUser() {
		return trueUser;
	}

	public void setTrueUser(UserDomain trueUser) {
		this.trueUser = trueUser;
	}

	public List<Virtual> getVirtuals() {
		return virtuals;
	}

	public void setVirtuals(List<Virtual> virtuals) {
		this.virtuals = virtuals;
	}

	public static class Virtual implements Cloneable {
		/**
		 * {"virtualId":7,
		 * "trueUserId":"159b6a505277b6f39655cbfcb83a84b4",
		 * "virtualUserAccount":"阿什顿",
		 * "webType":"3",
		 * "webName":"腾讯微博",
		 * "webSourceId":8,
		 * "lasttime":"2015-07-15 10:04:04.0"}
		 */

		@JsonProperty("virtualId")
		private int id;

		@JsonProperty("trueUserId")
		private String trueUser;

		@JsonProperty("virtualUserAccount")
		private String nickname;

		@JsonProperty("webType")
		private int platform;

		@JsonProperty("webName")
		private String source_name;

		@JsonProperty("webSourceId")
		private int source_id;

		@JsonProperty("lasttime")
		private String lasttime;

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return MoreObjects.toStringHelper(Virtual.class).add("nickname", nickname).add("source_name", source_name)
					.toString();
		}

		@Override
		protected Virtual clone() {
			// TODO Auto-generated method stub
			Virtual vir = null;
			try {
				vir = (Virtual) super.clone();
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return vir;
		}

		public String getNickname() {
			return nickname;
		}

		//		@JsonProperty("virtualUserAccount")
		public void setNickname(String nickname) {
			this.nickname = nickname;
		}


		public int getSource_id() {
			return source_id;
		}

		//		@JsonProperty("webSourceId")
		public void setSource_id(int source_id) {
			this.source_id = source_id;
		}


		public int getId() {
			return id;
		}

		//		@JsonProperty("virtualId")
		public void setId(int id) {
			this.id = id;
		}


		public int getPlatform() {
			return platform;
		}

		//		@JsonProperty("webType")
		public void setPlatform(int platform) {
			this.platform = platform;
		}


		public String getTrueUser() {
			return trueUser;
		}

		//		@JsonProperty("trueUserId")
		public void setTrueUser(String trueUser) {
			this.trueUser = trueUser;
		}

		public String getSource_name() {
			return source_name;
		}

		public void setSource_name(String source_name) {
			this.source_name = source_name;
		}

		public String getLasttime() {
			return lasttime;
		}

		public void setLasttime(String lasttime) {
			this.lasttime = lasttime;
		}

	}
}
