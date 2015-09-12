package zx.soft.sent.solr.insight;

import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.common.insight.AreaCode;
import zx.soft.sent.common.insight.TrueUserHelper;
import zx.soft.sent.common.insight.UserDomain;
import zx.soft.sent.common.insight.Virtuals.Virtual;
import zx.soft.sent.core.domain.QueryParams;
import zx.soft.sent.solr.domain.QueryResult;
import zx.soft.sent.solr.query.QueryCore;
import zx.soft.utils.http.HttpClientDaoImpl;
import zx.soft.utils.json.JsonNodeUtils;
import zx.soft.utils.log.LogbackUtil;
import zx.soft.utils.string.ConcatMethod;
import zx.soft.utils.string.StringConcatHelper;
import zx.soft.utils.time.TimeUtils;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 重点人员分时段统计热们关键词：hefei06
 *
 * 运行目录：/home/zxdfs/run-work/timer/insight
 * 运行命令：./insighthotkey.sh &
 * @author donglei
 *
 */
public class UserActivity {

	private static Logger logger = LoggerFactory.getLogger(UserActivity.class);

	public static final String ACTIVITY_URL = "http://192.168.32.20:8080/keyusers/updateTrueActive";

	public UserActivity() {
	}

	/**
	 * 主函数
	 */
	public static void main(String[] args) {
		UserActivity activityRun = new UserActivity();
		try {
			activityRun.run();
		} catch (Exception e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
		}
	}

	public void run() {
		StringConcatHelper helper = new StringConcatHelper(ConcatMethod.OR);
		long currentTime = System.currentTimeMillis();
		long startTime = TimeUtils.getMidnight(currentTime, 0);
		QueryParams params = new QueryParams();
		params.setQ("*:*");
		params.setRows(0);
		params.setFq("timestamp:[" + TimeUtils.transToSolrDateStr(startTime) + " TO "
				+ TimeUtils.transToSolrDateStr(currentTime) + "]");
		for (AreaCode area : AreaCode.values()) {
			List<UserDomain> trueUsers = TrueUserHelper.getTrueUsers(area.getAreaCode());
			for (UserDomain user : trueUsers) {
				helper.clear();
				String trueUserId = user.getTureUserId();
				List<Virtual> virtuals = TrueUserHelper.getVirtuals(trueUserId);
				if (!virtuals.isEmpty()) {
					for (Virtual virtual : virtuals) {
						helper.add("(nickname:\"" + virtual.getNickname() + "\" AND source_id:"
								+ virtual.getSource_id() + ")");
					}
					QueryParams tmp = params.clone();
					tmp.setFq(params.getFq() + ";" + helper.getString());
					QueryResult result = QueryCore.getInstance().queryData(tmp, false);
					HttpClientDaoImpl client = new HttpClientDaoImpl();
					String response = client.doPostAndGetResponse(ACTIVITY_URL, "{\"tureUserId\":\"" + trueUserId
							+ "\",\"pre_count\":" + result.getNumFound() + "}");
					JsonNode errorResponse = JsonNodeUtils
							.getJsonNode(JsonNodeUtils.getJsonNode(response), "errorCode");
					if (errorResponse.intValue() != 0) {
						logger.info(response);
					}
				}
			}
		}
		// 关闭资源
		QueryCore.getInstance().close();
		logger.info("Finishing query OA-FirstPage data...");
	}

	public static long getHourTime(long milli) {
		Calendar date = Calendar.getInstance();
		date.setTimeInMillis(milli);
		date.set(Calendar.MINUTE, 0);
		date.set(Calendar.SECOND, 0);
		date.set(Calendar.MILLISECOND, 0);
		return date.getTimeInMillis();
	}

}
