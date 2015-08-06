package zx.soft.sent.insight.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import zx.soft.sent.solr.insight.UserDomain;
import zx.soft.sent.solr.insight.Virtuals;
import zx.soft.sent.solr.insight.Virtuals.Virtual;

public class VirtualsCache {

	private static final long CACHE_PERIOD = 5 * 60 * 1000;
	private static final int CACHE_ITEM_COUNT = 50;
	private static Map<String, Virtuals> caches = new HashMap<String, Virtuals>();

	public static void addVirtuals(String trueUser, List<Virtual> virtuals) {
		synchronized (caches) {
			clearCache();
			if (caches.containsKey(trueUser)) {
				Virtuals virs = caches.get(trueUser);
				virs.setTimestamp(System.currentTimeMillis());
				virs.setVirtuals(virtuals);
			} else {
				if (caches.entrySet().size() < CACHE_ITEM_COUNT) {
					caches.put(trueUser, new Virtuals(System.currentTimeMillis(), virtuals));
				}
			}

		}
	}

	public static void addTrueUser(String trueUserId, UserDomain user) {
		synchronized (caches) {
			clearCache();
			if (caches.containsKey(trueUserId)) {
				Virtuals virs = caches.get(trueUserId);
				virs.setTimestamp(System.currentTimeMillis());
				virs.setTrueUser(user);
			} else {
				if (caches.entrySet().size() < CACHE_ITEM_COUNT) {
					caches.put(trueUserId, new Virtuals(System.currentTimeMillis(), user));
				}
			}
		}
	}

	public static Virtuals getVirtuals(String trueUser) {
		synchronized (caches) {
			clearCache();
			if (caches.containsKey(trueUser)) {
				return caches.get(trueUser).clone();
			} else {
				return null;
			}
		}
	}

	private static void clearCache() {
		Iterator<Entry<String, Virtuals>> iterator = caches.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, Virtuals> entry = iterator.next();
			if (System.currentTimeMillis() - entry.getValue().getTimestamp() > CACHE_PERIOD) {
				iterator.remove();
			}
		}
	}
}
