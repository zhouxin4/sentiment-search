package zx.soft.sent.solr.insight;

public enum AreaCode {
	ANHUI("340000"), HEFEI("340100"), NAQI("340800"), HAINAN("340400");
	private String areaCode;

	AreaCode(String code) {
		this.areaCode = code;
	}

	public String getAreaCode() {
		return areaCode;
	}

}
