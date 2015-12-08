package zx.soft.sent.solr.insight;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.client.solrj.response.RangeFacet.Count;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.common.domain.QueryParams;
import zx.soft.sent.common.insight.AreaCode;
import zx.soft.sent.common.insight.TrueUserHelper;
import zx.soft.sent.common.insight.UserDomain;
import zx.soft.sent.common.insight.Virtuals.Virtual;
import zx.soft.sent.dao.insight.RiakInsight;
import zx.soft.sent.solr.domain.QueryResult;
import zx.soft.sent.solr.query.QueryCore;
import zx.soft.utils.json.JsonUtils;
import zx.soft.utils.log.LogbackUtil;
import zx.soft.utils.time.TimeUtils;

public class PostCache {

	private static Logger logger = LoggerFactory.getLogger(PostCache.class);

	private RiakInsight insight = null;

	public PostCache() {
		insight = new RiakInsight();
	}

	public static void main(String[] args) {
		PostCache cache = new PostCache();
		cache.run();
		cache.insight.close();
	}

	private void run() {
		for (AreaCode area : AreaCode.values()) {
			String areaCode = area.getAreaCode();
			List<UserDomain> trueUsers = TrueUserHelper.getTrueUsers(areaCode);
			for (UserDomain user : trueUsers) {
				String trueUserId = user.getTureUserId();
				List<Virtual> virtuals = TrueUserHelper.getVirtuals(trueUserId);
				for (Virtual virtual : virtuals) {
					Calendar date = Calendar.getInstance();
					date.set(date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, 1, 0, 0, 0);
					date.set(Calendar.MILLISECOND, 0);
					long current = date.getTimeInMillis();
					for (int i = 0; i < 4; i++) {
						try {
							long zeroMonth = TimeUtils.transCurrentTime(current, 0, -1, 0, 0);
							QueryParams queryParams = new QueryParams();
							queryParams.setQ("*:*");
							queryParams.setRows(0);
							queryParams.setFacetRange("timestamp");
							queryParams.setFq("timestamp:[" + TimeUtils.transToSolrDateStr(zeroMonth) + " TO "
									+ TimeUtils.transToSolrDateStr(current) + "];nickname:\"" + virtual.getNickname()
									+ "\";source_id:" + virtual.getSource_id());
							queryParams.setFacetRangeStart(TimeUtils.transToSolrDateStr(zeroMonth));
							queryParams.setFacetRangeEnd(TimeUtils.transToSolrDateStr(current));
							queryParams.setFacetRangeGap("+1HOUR");
							QueryResult result = QueryCore.getInstance().queryData(queryParams, false);
							logger.info(JsonUtils.toJsonWithoutPretty(queryParams));
							Map<String, Integer> maps = new HashMap<>();
							for (RangeFacet facet : result.getFacetRanges()) {
								if ("timestamp".equals(facet.getName())) {
									List<Count> counts = facet.getCounts();
									for (Count count : counts) {
										maps.put(TimeUtils.timeStrByHour(TimeUtils.tranSolrDateStrToMilli(count
												.getValue())), count.getCount());
									}
								}
							}
							this.insight.insertHotkeys("post",
									trueUserId + "_" + virtual.getNickname() + "_" + virtual.getSource_id() + "_"
											+ TimeUtils.timeStrByHour(zeroMonth), JsonUtils.toJsonWithoutPretty(maps));
							current = zeroMonth;
						} catch (Exception e) {
							logger.info(LogbackUtil.expection2Str(e));
						}
					}
				}
			}
		}
	}
}
