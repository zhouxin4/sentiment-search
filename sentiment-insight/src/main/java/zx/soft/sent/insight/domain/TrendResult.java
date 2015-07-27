package zx.soft.sent.insight.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Longs;

public class TrendResult {
	private Map<String, Long> trends = new HashMap<>();
	private List<Map.Entry<String, Long>> sortedhotKeys = new ArrayList<>();
	@JsonIgnore
	private Map<String, Long> hotkeys = new HashMap<>();

	public Map<String, Long> getTrends() {
		return trends;
	}

	public void setTrends(Map<String, Long> trends) {
		this.trends = trends;
	}

	public List<Map.Entry<String, Long>> getSortedhotKeys() {
		return sortedhotKeys;
	}

	public void setSortedhotKeys(List<Map.Entry<String, Long>> sortedhotKeys) {
		this.sortedhotKeys = sortedhotKeys;
	}

	public Map<String, Long> getHotkeys() {
		return hotkeys;
	}

	public void setHotkeys(Map<String, Long> hotkeys) {
		this.hotkeys = hotkeys;
	}

	private void countMapItem(Map<String, Long> maps, String key, long value) {
		Preconditions.checkArgument(!key.isEmpty());
		if (maps.containsKey(key)) {
			maps.put(key, maps.get(key) + value);
		} else {
			maps.put(key, value);
		}
	}

	public void addItem(String key, long value) {
		Preconditions.checkNotNull(key);
		String tmp[] = key.split("##");
		Preconditions.checkArgument(tmp.length == 2);
		countMapItem(trends, tmp[0], value);
		countMapItem(hotkeys, tmp[1], value);
	}

	public void sortHotKeys() {
		for (Entry<String, Long> entry : hotkeys.entrySet()) {
			sortedhotKeys.add(entry);
		}
		Collections.sort(sortedhotKeys, new Comparator<Entry<String, Long>>() {

			@Override
			public int compare(Entry<String, Long> o1, Entry<String, Long> o2) {
				return -Longs.compare(o1.getValue(), o2.getValue());
			}
		});
	}


}
