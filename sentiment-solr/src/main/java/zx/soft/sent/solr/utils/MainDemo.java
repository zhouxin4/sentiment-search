package zx.soft.sent.solr.utils;

public class MainDemo {

	public static void main(String[] args) {
		System.out.println(StringUtils.getLevenshteinDistance(
				"安庆市公安局宜秀分局刑警大队2010年11月成功抓获了刘宏，至今却没有拿到... 刘宏伙同他人开车来到上海医药公司安庆分公司的一个药房仓库，撬门入室盗走",
				"安庆市公安局宜秀分局刑警大队2010年11月成功抓获了刘宏，却没有拿到... 刘宏伙同他人开车来到上海医药公司安庆分公司的一个药房仓库，撬门入走"));
		System.out.println(StringUtils.getLevenshteinDistance("elephant", "hippo")); //7
		System.out.println(StringUtils.getLevenshteinDistance("hippo", "elephant")); //7
		System.out.println(StringUtils.getLevenshteinDistance("hippo", "zzzzzzzz")); //8
		System.out.println(StringUtils.getLevenshteinDistance("hello", "hallo")); //1
	}
}
