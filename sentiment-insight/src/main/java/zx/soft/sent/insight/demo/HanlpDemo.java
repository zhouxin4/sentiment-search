package zx.soft.sent.insight.demo;

import java.util.List;

import com.hankcs.hanlp.HanLP;

/**
 * 关键词提取
 * @author donglei
 *
 */
public class HanlpDemo {
	public static void main(String[] args) {
		String content = "，目前多方人员均难以和其取得联系。";
		List<String> keywordList = HanLP.extractKeyword(content, 20);
		System.out.println(keywordList);
	}
}
