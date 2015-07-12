package zx.soft.sent.spring.utils;

import java.util.concurrent.BlockingQueue;

import org.eclipse.jetty.util.BlockingArrayQueue;

public class StringCache {

	private static BlockingQueue<String> tokenQueue = new BlockingArrayQueue<String>();

	public static boolean addToken(String token) {
		return tokenQueue.offer(token);
	}

	public static String getToken() throws InterruptedException {
		return tokenQueue.take();
	}

}
