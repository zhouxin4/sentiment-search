package zx.soft.sent.solr.firstpage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.negative.sentiment.core.NegativeClassify;
import zx.soft.sent.dao.firstpage.FirstPagePersistable;
import zx.soft.sent.dao.firstpage.RiakFirstPage;
import zx.soft.utils.algo.TopN;
import zx.soft.utils.checksum.CheckSumUtils;
import zx.soft.utils.json.JsonUtils;
import zx.soft.utils.log.LogbackUtil;
import zx.soft.utils.sort.InsertSort;

/**
 * OA首页信息定时分析：hefei07
 *
 * 运行目录：/home/zxdfs/run-work/timer/oa-firstpage
 * 注： 老版本访问数据库firstpagerun、firstpageharmfulRun;分别运行在oa-firstpage、oa-firstpageharmful
 *     *中间状态oa-firstpage仍然保留数据库的版本firstpagerun。firstpagerun、firstpageharmfulRun合并部署在oa-firstpageharmful
 *     最终状态： 无oa-firstpageharmful，合并后部署在oa-firstpage
 * 运行命令：./firstpage_timer.sh &
 *
 * 广西： gxqt6
 * 运行目录：/home/solr/run-work/timer/oa-firstpage
 *
 * @author donglei
 *
 */
public class FirstPageRun {

	private static Logger logger = LoggerFactory.getLogger(FirstPageRun.class);

	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd,HH");

	public FirstPageRun() {
		//
	}

	/**
	 * 主函数
	 */
	public static void main(String[] args) {
		FirstPageRun firstPageRun = new FirstPageRun();
		try {
			firstPageRun.run();
		} catch (Exception e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
		}
	}

	public void run() {
		logger.info("Starting query OA-FirstPage data...");
		//		FirstPagePersistable firstPage = new FirstPage(MybatisConfig.ServerEnum.sentiment);
		FirstPagePersistable firstPage = new RiakFirstPage();
		OAFirstPage oafirstPage = new OAFirstPage();
		NegativeClassify negativeClassify = new NegativeClassify();
		/**
		 * 1、统计当前时间各类数据的总量
		 */
		HashMap<String, Long> currentPlatformSum = oafirstPage.getCurrentPlatformSum();
		firstPage.insertFirstPage(1, timeStrByHour(), JsonUtils.toJsonWithoutPretty(currentPlatformSum));
		/**
		 * 2、统计当天各类数据的进入量，其中day=0表示当天的数据
		 */
		HashMap<String, Long> todayPlatformInputSum = oafirstPage.getTodayPlatformInputSum(0);
		firstPage.insertFirstPage(2, timeStrByHour(), JsonUtils.toJsonWithoutPretty(todayPlatformInputSum));
		/**
		 * 4、根据当天的微博数据，分别统计0、3、6、9、12、15、18、21时刻的四大微博数据进入总量；
		 * 即从0点开始，每隔3个小时统计以下，如果当前的小时在这几个时刻内就统计，否则不统计。
		 */
		int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		if (hour % 3 == 0) {
			HashMap<String, Long> todayWeibosSum = oafirstPage.getTodayWeibosSum(0, hour);
			firstPage.insertFirstPage(4, timeStrByHour(), JsonUtils.toJsonWithoutPretty(todayWeibosSum));
		}
		/**
		 * 5、对当天的论坛和微博进入数据进行负面评分，并按照分值推送最大的签20条内容，每小时推送一次。
		 * @param platform:论坛-2,微博-3
		 */
		List<SolrDocument> negativeRecordsForum = oafirstPage.getNegativeRecords(2, 0, 10);
		List<SolrDocument> negativeRecordsWeibo = oafirstPage.getNegativeRecords(3, 0, 10);
		negativeRecordsForum = getTopNNegativeRecords(negativeClassify, negativeRecordsForum, 20);
		negativeRecordsWeibo = getTopNNegativeRecords(negativeClassify, negativeRecordsWeibo, 20);
		firstPage.insertFirstPage(52, timeStrByHour(), JsonUtils.toJsonWithoutPretty(negativeRecordsForum));
		firstPage.insertFirstPage(53, timeStrByHour(), JsonUtils.toJsonWithoutPretty(negativeRecordsWeibo));

		/**
		 * 对当天的各平台进入数据进行负面评分，并按照分值推送最大的签20条内容，每小时推送一次。
		 */
		/*for (int i = 1; i < SentimentConstant.PLATFORM_ARRAY.length; i++) {
			logger.info("Retriving platform:{}", i);
			List<SolrDocument> negativeRecordsForum = oafirstPage.getNegativeRecords(i, 0, 30);
			negativeRecordsForum = FirstPageRun.getTopNNegativeRecords(negativeClassify, negativeRecordsForum, 50);
			firstPage.insertFirstPage(i, FirstPageRun.timeStrByHour(),
					JsonUtils.toJsonWithoutPretty(negativeRecordsForum));
		}*/

		negativeRecordsForum = oafirstPage.getHarmfulRecords("1,2,3,4,7,10", 0, 30);
		negativeRecordsForum = FirstPageRun.getNewTopNNegativeRecords(negativeClassify, negativeRecordsForum, 50);
		firstPage.insertFirstPage(0, FirstPageRun.timeStrByHour(), JsonUtils.toJsonWithoutPretty(negativeRecordsForum));
		//		System.out.println(JsonUtils.toJson(negativeRecordsForum));

		// 关闭资源
		firstPage.close();
		negativeClassify.cleanup();
		oafirstPage.close();
		logger.info("Finishing query OA-FirstPage data...");
	}

	/**
	 * 将当前的时间戳转换成小时精度，如："2014-09-05,14"
	 */
	public static String timeStrByHour() {
		return FORMATTER.format(new Date());
	}

	public static List<SolrDocument> getNewTopNNegativeRecords(NegativeClassify negativeClassify,
			List<SolrDocument> records, int N) {
		List<SolrDocument> docs = TopN.topNUnique(records, N, new MyComparator(negativeClassify));
		return docs;
	}

	/**
	 * 排序计算，得到前20负面信息
	 * @param records
	 * @param N
	 * @return
	 */
	public static List<SolrDocument> getTopNNegativeRecords(NegativeClassify negativeClassify,
			List<SolrDocument> records, int N) {
		List<SolrDocument> result = new ArrayList<>();
		HashSet<String> urls = new HashSet<>();
		String[] insertTables = new String[records.size()];
		for (int i = 0; i < records.size(); i++) {
			String str = "";
			if (records.get(i).get("title") != null) {
				str += records.get(i).get("title").toString();
			}
			if (records.get(i).get("content") != null) {
				str += records.get(i).get("content").toString();
			}
			insertTables[i] = i + "=" + (int) negativeClassify.getTextScore(str);
		}
		String[] table = new String[records.size()];
		for (int i = 0; i < table.length; i++) {
			table[i] = "0=0";
		}
		for (int i = 0; i < table.length; i++) {
			table = InsertSort.toptable(table, insertTables[i]);
		}
		String[] keyvalue = null;
		for (int i = 0; result.size() < Math.min(table.length, N) && i < table.length; i++) {
			keyvalue = table[i].split("=");
			SolrDocument doc = records.get(Integer.parseInt(keyvalue[0]));
			doc.setField("score", keyvalue[1]);
			if (urls.contains(CheckSumUtils.getMD5(doc.getFieldValue("content").toString()))) {
				continue;
			}
			urls.add(CheckSumUtils.getMD5(doc.getFieldValue("content").toString()));
			result.add(doc);
		}

		return result;
	}

	static class MyComparator implements Comparator<SolrDocument> {

		private HashMap<String, Float> hashs;
		private NegativeClassify negativeClassify;

		public MyComparator(NegativeClassify negativeClassify) {
			hashs = new HashMap<>();
			this.negativeClassify = negativeClassify;
		}

		@Override
		public int compare(SolrDocument o1, SolrDocument o2) {
			float score1 = 0;
			float score2 = 0;
			if (o1.getFieldValue("content") == null) {
				return -1;
			}
			if (o2.getFieldValue("content") == null) {
				return 1;
			}
			String md1 = CheckSumUtils.getMD5(o1.getFieldValue("content").toString());
			String md2 = CheckSumUtils.getMD5(o2.getFieldValue("content").toString());
			if (!hashs.containsKey(md1)) {
				score1 = getScore(o1);
				hashs.put(md1, score1);
			} else {
				score1 = hashs.get(md1);
			}
			if (!hashs.containsKey(md2)) {
				score2 = getScore(o2);
				hashs.put(md2, score2);
			} else {
				score2 = hashs.get(md2);
			}
			return score1 - score2 > 0 ? 1 : score1 - score2 < 0 ? -1 : 0;

		}

		public float getScore(SolrDocument o1) {
			String str = "";
			if (o1.get("title") != null) {
				str += o1.get("title").toString();
			}
			if (o1.get("content") != null) {
				str += o1.get("content").toString();
			}
			if (str.isEmpty()) {
				return 0;
			}
			float score = negativeClassify.getTextScore(str);
			float rate = (float) Math.log10(str.length());
			o1.setField("score", (int) Math.ceil((score / rate)));
			return score / rate;
		}
	}

}
