package zx.soft.sent.insight.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import zx.soft.utils.string.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.primitives.Ints;

public class TrendResult {
	private Map<String, Long> trends = new HashMap<>();
	private List<Map.Entry<String, Integer>> sortedhotKeys = new ArrayList<>();
	@JsonIgnore
	private Multiset<String> hotkeys = HashMultiset.create();

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(TrendResult.class).add("trends", trends).add("sortedhotKeys", sortedhotKeys)
				.toString();
	}

	public void setTrends(Map<String, Long> trends) {
		this.trends = trends;
	}

	public List<Map.Entry<String, Integer>> getSortedhotKeys() {
		return sortedhotKeys;
	}

	public void setSortedhotKeys(List<Map.Entry<String, Integer>> sortedhotKeys) {
		this.sortedhotKeys = sortedhotKeys;
	}

	public Map<String, Long> getTrends() {
		return trends;
	}

	public void countTrend(String key, long value) {
		Preconditions.checkArgument(!key.isEmpty());
		if (trends.containsKey(key)) {
			trends.put(key, trends.get(key) + value);
		} else {
			trends.put(key, value);
		}
	}

	public void countHotWords(String key, int count) {
		if (!StringUtils.isEmpty(key)) {
			hotkeys.add(key, count);
		}
	}

	public void sortHotKeys() {
		Map<String, Integer> counts = Maps.newHashMap();

		for (Entry<String> entry : hotkeys.entrySet()) {
			counts.put(entry.getElement(), entry.getCount());
		}
		for (Map.Entry<String, Integer> keyvalue : counts.entrySet()) {
			sortedhotKeys.add(keyvalue);
		}
		Collections.sort(sortedhotKeys, new Comparator<Map.Entry<String, Integer>>() {

			@Override
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				return -Ints.compare(o1.getValue(), o2.getValue());
			}
		});
		if (sortedhotKeys.size() > 20) {
			this.sortedhotKeys = sortedhotKeys.subList(0, 20);
		}
	}


}
