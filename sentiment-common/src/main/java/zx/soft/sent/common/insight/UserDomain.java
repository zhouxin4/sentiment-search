package zx.soft.sent.common.insight;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.MoreObjects;

@JsonIgnoreProperties({ "qq", "phone", "email", "selfEdit" })
public class UserDomain implements Cloneable {
	private String userName;
	private String identityCard;
	private int sex;
	private String nation;
	private String nativePlace;
	private String address;
	private String company;
	private String imageUrl;
	private String introduction;
	private String areaCode;
	private String createArea;
	private String createUser;
	private int pre_count;
	private String tureUserId;
	private boolean user;

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(UserDomain.class).add("tureUserId", tureUserId).add("userName", userName)
				.toString();
	}

	@Override
	protected UserDomain clone() {
		UserDomain user = null;
		try {
			user = (UserDomain) super.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return user;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getIdentityCard() {
		return identityCard;
	}

	public void setIdentityCard(String identityCard) {
		this.identityCard = identityCard;
	}

	public int getSex() {
		return sex;
	}

	public void setSex(int sex) {
		this.sex = sex;
	}

	public String getNation() {
		return nation;
	}

	public void setNation(String nation) {
		this.nation = nation;
	}

	public String getNativePlace() {
		return nativePlace;
	}

	public void setNativePlace(String nativePlace) {
		this.nativePlace = nativePlace;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getIntroduction() {
		return introduction;
	}

	public void setIntroduction(String introduction) {
		this.introduction = introduction;
	}

	public String getAreaCode() {
		return areaCode;
	}

	public void setAreaCode(String areaCode) {
		this.areaCode = areaCode;
	}

	public String getCreateArea() {
		return createArea;
	}

	public void setCreateArea(String createArea) {
		this.createArea = createArea;
	}

	public String getCreateUser() {
		return createUser;
	}

	public void setCreateUser(String createUser) {
		this.createUser = createUser;
	}

	public int getPre_count() {
		return pre_count;
	}

	public void setPre_count(int pre_count) {
		this.pre_count = pre_count;
	}

	public String getTureUserId() {
		return tureUserId;
	}

	public void setTureUserId(String tureUserId) {
		this.tureUserId = tureUserId;
	}

	public boolean isUser() {
		return user;
	}

	public void setUser(boolean user) {
		this.user = user;
	}

	public static void main(String[] args) {
		UserDomain domain = new UserDomain();
		domain.setTureUserId("12312312312312");
		domain.setUserName("zhangsan");
		System.out.println(domain);
	}

}
