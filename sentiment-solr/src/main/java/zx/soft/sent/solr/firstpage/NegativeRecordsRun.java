package zx.soft.sent.solr.firstpage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.negative.sentiment.core.NegativeClassify;
import zx.soft.sent.common.domain.SentimentConstant;
import zx.soft.sent.solr.utils.RedisMQ;
import zx.soft.utils.algo.TopN;
import zx.soft.utils.checksum.CheckSumUtils;
import zx.soft.utils.json.JsonUtils;
import zx.soft.utils.log.LogbackUtil;
import zx.soft.utils.sort.InsertSort;

/**
 * OA首页信息定时分析：hefei07
 *
 * 运行目录：/home/zxdfs/run-work/timer/oa-firstpage
 * 运行命令：./firstpage_timer.sh &
 *
 * @author donglei
 *
 */
public class NegativeRecordsRun {

	private static Logger logger = LoggerFactory.getLogger(NegativeRecordsRun.class);

	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd,HH");

	public NegativeRecordsRun() {
		//
	}

	/**
	 * 主函数
	 */
	public static void main(String[] args) {
		NegativeRecordsRun firstPageRun = new NegativeRecordsRun();
		try {
			firstPageRun.run();
		} catch (Exception e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
		}
	}

	public void run() {
		logger.info("Starting query OA-FirstPage data...");
		OAFirstPage oafirstPage = new OAFirstPage();
		NegativeClassify negativeClassify = new NegativeClassify();
		RedisMQ redisMQ = new RedisMQ();

		List<SolrDocument> negativeRecordsForum = oafirstPage.getHarmfulRecords_N("1,2,3,4,7,10", 0, 50);
		negativeRecordsForum = NegativeRecordsRun
				.getNewTopNNegativeRecords(negativeClassify, negativeRecordsForum, 100);
		List<String> jsonDocs = new ArrayList<>();
		for (SolrDocument doc : negativeRecordsForum) {
			jsonDocs.add(JsonUtils.toJsonWithoutPretty(doc));
		}
		redisMQ.addRecord(SentimentConstant.CDH5_CACHE_RECORDS, jsonDocs.toArray(new String[jsonDocs.size()]));

		// 关闭资源
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
			//		int rate = str.length() / 20 + 1;
			int rate = (int) Math.log10(str.length()) + 1;
			o1.setField("cache_value", (int) (score / rate));
			return score / rate;
		}
	}

}