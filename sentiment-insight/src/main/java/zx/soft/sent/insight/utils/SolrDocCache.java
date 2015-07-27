package zx.soft.sent.insight.utils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

public class SolrDocCache {
	private static Logger logger = LoggerFactory.getLogger(SolrDocCache.class);
	private Cache<Integer, SolrDocument> cache;
	private AtomicInteger atomInt;

	public SolrDocCache() {
		atomInt = new AtomicInteger(0);
		cache = CacheBuilder.newBuilder()
		//设置并发级别为8，并发级别是指可以同时写缓存的线程数
				.concurrencyLevel(8)
				//设置写缓存后8秒钟过期
				.expireAfterWrite(5, TimeUnit.MINUTES)
				//设置缓存容器的初始容量为10
				.initialCapacity(10)
				//设置缓存最大容量为100，超过100之后就会按照LRU最近虽少使用算法来移除缓存项
				.maximumSize(200)
				//设置缓存的移除通知  RemovalListener同步执行   异步执行可以RemovalListeners.asynchronous(RemovalListener, Executor)
				.removalListener(new RemovalListener<Object, Object>() {
					@Override
					public void onRemoval(RemovalNotification<Object, Object> notification) {
						logger.info("{} was removed, cause is {}", notification.getKey(), notification.getCause());
					}
				})
				//build方法中可以指定CacheLoader，在缓存不存在时通过CacheLoader的实现自动加载缓存
				.build();
	}

	public int getAndIncrement() {
		return atomInt.incrementAndGet();
	}

	public void initAtoInt(int newValue) {
		atomInt.getAndSet(newValue);
	}

	public void putSolrDocument(SolrDocument doc) {
		Preconditions.checkNotNull(doc);
		cache.put(atomInt.intValue(), doc);
		atomInt.incrementAndGet();
	}

	public SolrDocument getSolrDocument(int index) {
		return cache.getIfPresent(index);
	}

	public void clear() {
		cache.cleanUp();
	}

	public static void main(String[] args) {
		SolrDocCache docCache = new SolrDocCache();
		SolrDocument doc = null;
		try {
			doc = docCache.getSolrDocument(1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(doc);
		SolrDocument doc1 = new SolrDocument();
		docCache.putSolrDocument(doc1);
		SolrDocument doc2 = null;
		try {
			doc2 = docCache.getSolrDocument(0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(doc2);

	}

}

