package zx.soft.sent.spring.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.utils.chars.CharsetEncoding;

public class TokenPersist implements Runnable {
	private static Logger logger = LoggerFactory.getLogger(TokenPersist.class);
	private static TokenPersist persist = new TokenPersist();

	private TokenPersist() {

	}

	public static TokenPersist getInstance() {
		return persist;
	}

	@Override
	public void run() {
		logger.info("TokenPersist start...");
		String token = null;
		try {
			while (!(token = StringCache.getToken()).isEmpty()) {
				printStringToFile(CharsetEncoding.toUTF_8(token));
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.info("TokenPersist exit...");
	}

	public void printStringToFile(String token) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileWriter("dics/words-sentiment.dic", true));
			pw.println(token);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			pw.close();
		}

	}
}
