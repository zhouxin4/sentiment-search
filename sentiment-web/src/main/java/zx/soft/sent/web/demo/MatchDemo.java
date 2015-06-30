package zx.soft.sent.web.demo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import zx.soft.utils.http.HttpClientDaoImpl;
import zx.soft.utils.json.JsonUtils;

public class MatchDemo {

	private static void readFile(String file) {
		try {
			BufferedReader scan = null;
			try {
				scan = new BufferedReader(new FileReader(file));
				String s = scan.readLine();
				while (s != null) {
					System.out.println(s);
					String json;
					try {
						json = createJson(s);
						HttpClientDaoImpl http = new HttpClientDaoImpl();
						http.doPost("http://192.168.32.11:6900/site", json);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					s = scan.readLine();
				}
			} finally {
				if (scan != null) {
					scan.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		readFile("XTGL_YMLB.csv");

		//		try {
		//			String s = null;
		//			s = createJson("#合肥网站:#[]\\;/.,<>?:{}|#[]/.,<>?:");
		//			System.err.println(s);
		//			s = createJson("#合肥论坛类:429,296#芜湖论坛类:347#蚌埠论坛类:445");
		//			System.err.println(s);
		//			s = createJson("#合肥论坛类12:429,296#芜湖论坛类123:347#蚌埠论坛类213:445");
		//			System.err.println(s);
		//		} catch (Exception e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
	}

	private static String createJson(String line) throws Exception {
		String head = "#(.*?):";
		Pattern rHead = Pattern.compile(head);
		Matcher mHead = rHead.matcher(line);
		String result = line;
		while (mHead.find()) {
			String lHead = mHead.group();
			System.out.println(lHead);
			result = result.replaceFirst(lHead, "#:");
		}
		System.out.println(result);

		// 创建 Pattern 对象
		String pattern = "(\\d+)(\\,\\d+)*";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(result);

		// 现在创建 matcher 对象
		List<String> strs = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		while (m.find()) {
			String tmp = m.group(0);
			System.err.println("Found value: " + tmp);
			strs.add(tmp);
			sb.append(tmp);
			sb.append(",");
		}
		if (sb.length() != 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		if (strs.size() > 1) {
			strs.add(sb.toString());
		}
		return JsonUtils.toJsonWithoutPretty(strs);
	}

}
