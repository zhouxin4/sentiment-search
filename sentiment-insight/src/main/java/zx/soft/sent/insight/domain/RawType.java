package zx.soft.sent.insight.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * 动态数据模型类
 *
 * @author donglei
 *
 */
public class RawType {


	// 请求参数
	private Map<String, String> queryParams = null;
	private Map<String, Long> datas = null;

	public RawType() {
		queryParams = new HashMap<String, String>();
		datas = new HashMap<>();
	}

	public Map<String, String> getQueryParams() {
		return queryParams;
	}

	public void setQueryParams(Map<String, String> queryParams) {
		this.queryParams = queryParams;
	}

	public Map<String, Long> getDatas() {
		return datas;
	}

	public void setDatas(Map<String, Long> datas) {
		this.datas = datas;
	}

	public void addQueryParam(String key, String value) {
		if (queryParams.containsKey(key)) {
			queryParams.put(key, queryParams.get(key) + " OR " + value);
		} else {
			queryParams.put(key, value);
		}
	}

	public void countData(String key, long value) {
		if(datas.containsKey(key)) {
			datas.put(key, datas.get(key) + value);
		}else {
			datas.put(key, value);
		}
	}

	public void removeData(String key) {
		datas.remove(key);
	}

}