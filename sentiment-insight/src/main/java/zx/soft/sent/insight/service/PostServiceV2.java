package zx.soft.sent.insight.service;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import zx.soft.sent.common.domain.QueryParams;
import zx.soft.sent.common.insight.PostsResult;
import zx.soft.sent.common.insight.TrueUserHelper;
import zx.soft.sent.common.insight.Virtuals.Virtual;
import zx.soft.sent.dao.insight.RiakInsight;
import zx.soft.utils.json.JsonUtils;
import zx.soft.utils.log.LogbackUtil;
import zx.soft.utils.time.TimeUtils;

import com.google.common.collect.Collections2;

/**
 * 发帖情况统计模块
 * @author donglei
 */
@Service
public class PostServiceV2 {

	private static Logger logger = LoggerFactory.getLogger(PostServiceV2.class);

	private static RiakInsight riak = null;
	static {
		riak = new RiakInsight();
	}

	public enum GAP {
		DAY, WEEK, MONTH;
	}

	@SuppressWarnings({ "unchecked" })
	public Object getNicknamePostInfos(QueryParams params, String nickname, GAP gap) {
		long start = System.currentTimeMillis();
		PostsResult postsResults = new PostsResult();
		// 初始化postResult
		String fq = params.getFq();
		if (fq.contains("source_id")) {
			for (String sp : fq.split(";")) {
				if (sp.contains("source_id")) {
					postsResults.addDistIterm(sp.split(":")[1], 0);
				}
			}
		}
		for (int i = 0; i < 24; i++) {
			DecimalFormat format = (DecimalFormat) DecimalFormat.getIntegerInstance();
			format.applyPattern("00");
			postsResults.addHourIterm(format.format(i * 1.0), 0);
		}
		try {
			long startTime = TimeUtils.tranSolrDateStrToMilli(params.getFacetRangeStart());
			long endTime = TimeUtils.tranSolrDateStrToMilli(params.getFacetRangeEnd());
			while (startTime < endTime) {
				String[] tmp = TimeUtils.timeStrByHour(startTime).split(",");
				switch (gap) {
				case WEEK:
					postsResults.initDateIterm(tmp[0], 0);
					startTime = TimeUtils.transCurrentTime(startTime, 0, 0, 7, 0);
					break;
				case MONTH:
					String month = tmp[0].substring(0, tmp[0].lastIndexOf("-"));
					postsResults.initDateIterm(month, 0);
					startTime = TimeUtils.transCurrentTime(startTime, 0, 1, 0, 0);
					break;
				default:
					postsResults.initDateIterm(tmp[0], 0);
					startTime = TimeUtils.transCurrentTime(startTime, 0, 0, 1, 0);
				}
			}
		} catch (ParseException e) {
			logger.info(e.getMessage());
		}
		Collection<Virtual> virtuals = TrueUserHelper.getVirtuals(nickname);
		if (virtuals.isEmpty()) {
			logger.info("True user '{}': has no virtuals!", nickname);
			return postsResults;
		}
		virtuals = Collections2.filter(virtuals, new TrueUserHelper.VirtualPredicate(fq));

		for (Virtual virtual : virtuals) {
			try {
				long startTime = TimeUtils.tranSolrDateStrToMilli(params.getFacetRangeStart());
				long endTime = TimeUtils.tranSolrDateStrToMilli(params.getFacetRangeEnd());
				startTime = TimeUtils.getZeroHourTime(startTime);
				endTime = TimeUtils.getZeroHourTime(endTime);
				Map<String, Integer> monthMaps = null;
				int month = -1;
				while (startTime < endTime) {
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(startTime);
					String timeStr = TimeUtils.timeStrByHour(startTime);
					if (month != cal.get(Calendar.MONTH)) {
						month = cal.get(Calendar.MONTH);
						String tmp = riak.selectHotkeys(
								"post",
								nickname + "_" + virtual.getNickname() + "_" + virtual.getSource_id() + "_"
										+ timeStr.substring(0, timeStr.lastIndexOf("-")) + "-01,00");
						if (tmp != null) {
							monthMaps = JsonUtils.getObject(tmp, Map.class);
						}
					}
					if (monthMaps != null && monthMaps.get(timeStr) != null) {
						switch (gap) {
						case WEEK:
							postsResults.addDateIterm(timeStr.split(",")[0], monthMaps.get(timeStr));
							break;
						case MONTH:
							postsResults.addDateIterm(timeStr.substring(0, timeStr.lastIndexOf("-")),
									monthMaps.get(timeStr));
							break;
						default:
							postsResults.addDateIterm(timeStr.split(",")[0], monthMaps.get(timeStr));
						}
						postsResults.addHourIterm(timeStr.split(",")[1], monthMaps.get(timeStr));
						postsResults.addDistIterm(virtual.getSource_id() + "", monthMaps.get(timeStr));
					}

					startTime = TimeUtils.transCurrentTime(startTime, 0, 0, 0, 1);
				}
			} catch (Exception e) {
				logger.error(LogbackUtil.expection2Str(e));
			}
		}

		logger.info("统计发帖情况耗时: {}ms", System.currentTimeMillis() - start);
		return postsResults;
	}
}
