package zx.soft.sent.spring.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import zx.soft.utils.json.JsonUtils;

public class TokenUpdateData implements Serializable{
	private static final long serialVersionUID = 5504616187223776557L;
	private boolean intime;
	private List<String> tokens;

	//一定要有默认的构造器
	public TokenUpdateData() {

	}

	public boolean isIntime() {
		return intime;
	}

	public void setIntime(boolean intime) {
		this.intime = intime;
	}

	public List<String> getTokens() {
		return tokens;
	}

	public void setTokens(List<String> tokens) {
		this.tokens = tokens;
	}

	public static void main(String args[]) {
		List<String> tokens = new ArrayList<String>();
		tokens.add("区伯");
		tokens.add("八里河");
		TokenUpdateData tud = new TokenUpdateData();
		tud.setIntime(true);
		tud.setTokens(tokens);
		System.out.println(JsonUtils.toJsonWithoutPretty(tud));
	}

}
