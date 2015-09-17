package zx.soft.sent.insight.utils;

import java.text.ParseException;

import zx.soft.utils.time.TimeUtils;

public class MainTest {

	public static void main(String[] args) {
		final String timestamp = "2015-09-01T16:43:18Z";
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					System.out.println(TimeUtils.tranSolrDateStrToMilli(timestamp));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					System.out.println(TimeUtils.tranSolrDateStrToMilli(timestamp));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();

	}

}
