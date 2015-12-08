package zx.soft.sent.insight.demo;

import java.text.DecimalFormat;

import zx.soft.sent.insight.service.PostService.GAP;

/**
 * 关键词提取
 * @author donglei
 *
 */
public class HanlpDemo {
	public static void main(String[] args) {
		String gap = "DAY";
		GAP gaps = GAP.valueOf(gap);
		System.out.println(gaps);

		for (int i = 0; i < 24; i++) {
			//			System.out.println(String.format("%2d", i));
			DecimalFormat format = (DecimalFormat) DecimalFormat.getIntegerInstance();
			format.applyPattern("00");
			System.out.println(format.format(i * 1.0));

		}
	}
}
