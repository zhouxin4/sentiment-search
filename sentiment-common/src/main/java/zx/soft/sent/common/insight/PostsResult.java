package zx.soft.sent.common.insight;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.utils.json.JsonUtils;

/**
 * 重点人员发帖情况统计
 * @author donglei
 *
 */
public class PostsResult {

	private static Logger logger = LoggerFactory.getLogger(PostsResult.class);

	// 发帖日期趋势图
	private Map<String, Integer> trendOnDate = new TreeMap<>();
	// 发帖小时趋势图
	private Map<String, Integer> trendOnHour = new TreeMap<>();
	// 虚拟账号分布图
	private Map<String, Integer> virtuals = new HashMap<>();

	public Map<String, Integer> getVirtuals() {
		return virtuals;
	}

	public void setVirtuals(Map<String, Integer> virtuals) {
		this.virtuals = virtuals;
	}

	public Map<String, Integer> getTrendOnDate() {
		return trendOnDate;
	}

	public void setTrendOnDate(Map<String, Integer> trendOnDate) {
		this.trendOnDate = trendOnDate;
	}

	public Map<String, Integer> getTrendOnHour() {
		return trendOnHour;
	}

	public void setTrendOnHour(Map<String, Integer> trendOnHour) {
		this.trendOnHour = trendOnHour;
	}

	public void initDateIterm(String key, int value) {
		if (trendOnDate.containsKey(key)) {
			trendOnDate.put(key, trendOnDate.get(key) + value);
		} else {
			trendOnDate.put(key, value);
		}
	}

	public void addDateIterm(String key, int value) {
		String lastKey = null;
		for (String curKey : trendOnDate.keySet()) {
			if (curKey.compareTo(key) <= 0) {
				lastKey = curKey;
			}
		}
		if (lastKey == null) {
			trendOnDate.put(key, value);
		} else {
			trendOnDate.put(lastKey, trendOnDate.get(lastKey) + value);
		}
	}

	public void addHourIterm(String key, int value) {
		key = "tr_" + key;
		if (trendOnHour.containsKey(key)) {
			trendOnHour.put(key, trendOnHour.get(key) + value);
		} else {
			trendOnHour.put(key, value);
		}
	}

	public void addDistIterm(String key, int value) {
		if (virtuals.containsKey(key)) {
			virtuals.put(key, virtuals.get(key) + value);
		} else {
			virtuals.put(key, value);
		}
	}

	public void addIterm(String virtual, String solrTime, int value) {
		int i = solrTime.indexOf("-");
		int T = solrTime.indexOf("T");
		int k = solrTime.indexOf(':');
		addDateIterm(solrTime.substring(i + 1, T), value);
		addHourIterm(solrTime.substring(T + 1, k), value);
		addDistIterm(virtual, value);
	}

	public static void main(String[] args) {
		PostsResult date = new PostsResult();
		date.addDateIterm("01-01", 20);
		date.addDateIterm("02-06", 40);
		date.addDateIterm("01-03", 1);
		date.addDateIterm("01-04", 1);
		date.addDateIterm("02-04", 40);
		date.addDateIterm("02-01", 20);
		date.addDateIterm("02-02", 40);
		date.addDateIterm("01-05", 1);
		date.addDateIterm("02-03", 40);
		date.addDateIterm("01-02", 1);
		date.addDateIterm("02-05", 40);
		date.addDateIterm("02-06", 40);
		for (int i = 0; i < 10; i++) {
			date.addHourIterm("10", 1);
			date.addHourIterm("21", 1);
			date.addHourIterm("04", 1);
			date.addHourIterm("14", 1);
			date.addHourIterm("02", 1);
			date.addHourIterm("03", 1);
			date.addHourIterm("05", 1);
			date.addHourIterm("26", 1);
			date.addHourIterm("07", 1);
			date.addHourIterm("08", 1);
			date.addHourIterm("09", 1);
			date.addHourIterm("11", 1);
			date.addHourIterm("12", 1);
			date.addHourIterm("13", 1);
			date.addHourIterm("15", 1);
		}
		System.out.println(JsonUtils.toJson(date));
	}

}
