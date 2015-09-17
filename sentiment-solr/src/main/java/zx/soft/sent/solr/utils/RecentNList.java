package zx.soft.sent.solr.utils;

import java.util.LinkedList;
import java.util.List;

public class RecentNList<T> {

	private int num;

	private List<T> objects;

	public RecentNList() {
		objects = new LinkedList<>();
		num = Integer.MAX_VALUE;
	}

	public RecentNList(int n) {
		objects = new LinkedList<>();
		this.num = n;
	}

	public void addElement(T e) {
		if (objects.size() < num) {
			objects.add(0, e);
		} else {
			objects.remove(objects.size() - 1);
			objects.add(0, e);
		}
	}

	public List<T> getLists() {
		return objects;
	}
}
