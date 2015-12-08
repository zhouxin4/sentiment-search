package zx.soft.sent.insight.controller;

import java.text.ParseException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import zx.soft.sent.common.domain.ErrorResponse;
import zx.soft.sent.common.domain.QueryParams;
import zx.soft.sent.insight.domain.RelationRequest;
import zx.soft.sent.insight.domain.RelationRequest.EndPoint;
import zx.soft.sent.insight.service.PostServiceV2;
import zx.soft.sent.insight.service.PostServiceV2.GAP;
import zx.soft.sent.insight.service.QueryService;
import zx.soft.sent.insight.service.RelationServiceV2;
import zx.soft.sent.insight.service.TrendService;
import zx.soft.sent.solr.domain.QueryResult;
import zx.soft.utils.json.JsonUtils;
import zx.soft.utils.log.LogbackUtil;
import zx.soft.utils.time.TimeUtils;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 *
 * @author donglei
 *
 */
@Controller
@RequestMapping("/insights")
public class InsightController {
	Logger logger = LoggerFactory.getLogger(InsightController.class);

	@Inject
	private PostServiceV2 postService;
	@Inject
	private QueryService queryService;
	@Inject
	private RelationServiceV2 relationService;
	@Inject
	private TrendService trendService;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody Object getPostsResult(HttpServletRequest request) {
		String nickname = request.getParameter("trueUser");
		if (nickname == null || nickname.isEmpty()) {
			logger.error("Params `nickname` is null.");
			return new ErrorResponse.Builder(-1, "params error!").build();
		}

		QueryParams queryParams = new QueryParams();
		queryParams.setQ(request.getParameter("q") == null ? "*:*" : request.getParameter("q"));
		queryParams.setFq(request.getParameter("fq") == null ? "" : request.getParameter("fq"));
		queryParams.setRows(0);
		queryParams.setFacetRange(request.getParameter("facetRange") == null ? "timestamp" : request
				.getParameter("facetRange"));

		long endTime = System.currentTimeMillis();
		//		long startTime = TimeUtils.transCurrentTime(endTime, 0, 0, -7, 0);
		long startTime = TimeUtils.getMidnight(endTime, -6);
		try {
			endTime = request.getParameter("facetRangeEnd") == null ? endTime : TimeUtils
					.tranSolrDateStrToMilli(request.getParameter("facetRangeEnd"));
			startTime = request.getParameter("facetRangeStart") == null ? startTime : TimeUtils
					.tranSolrDateStrToMilli(request.getParameter("facetRangeStart"));
		} catch (ParseException e) {
			logger.info("请求参数时间格式不对!");
		}
		queryParams.setFq(queryParams.getFq() + ";timestamp:[" + TimeUtils.transToSolrDateStr(startTime) + " TO "
				+ TimeUtils.transToSolrDateStr(endTime) + "]");
		queryParams.setFacetRangeStart(TimeUtils.transToSolrDateStr(TimeUtils.getZeroHourTime(startTime)));
		queryParams.setFacetRangeEnd(TimeUtils.transToSolrDateStr(endTime));
		queryParams.setFacetRangeGap(request.getParameter("facetRangeGap") == null ? "+1HOUR" : request
				.getParameter("facetRangeGap"));

		GAP gap = GAP.DAY;
		String gapStr = request.getParameter("gap") == null ? "DAY" : request.getParameter("gap");
		try {
			gap = GAP.valueOf(gapStr);
		} catch (Exception e) {
			logger.error(LogbackUtil.expection2Str(e));
		}

		logger.info(queryParams.toString());

		return postService.getNicknamePostInfos(queryParams, nickname, gap);
	}

	@RequestMapping(value = "/trend", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody Object getTrendInfos(HttpServletRequest request) {
		String nickname = request.getParameter("trueUser");
		try {
			Preconditions.checkArgument(nickname != null && !nickname.isEmpty(), "Params `nickname` is null.");
		} catch (IllegalArgumentException e) {
			logger.error("Exception: {}", LogbackUtil.expection2Str(e));
			return new ErrorResponse.Builder(-1, "params error!").build();
		}
		QueryParams queryParams = new QueryParams();
		queryParams.setQ(request.getParameter("q") == null ? "*:*" : request.getParameter("q"));
		queryParams.setFq(request.getParameter("fq") == null ? "" : request.getParameter("fq"));
		queryParams.setQop("OR");
		logger.info(queryParams.toString());
		return trendService.getTrendInfos(queryParams, nickname);
	}

	@RequestMapping(value = "/query", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody Object queryData(HttpServletRequest request) {
		String nickname = request.getParameter("trueUser");
		if (nickname == null || nickname.isEmpty()) {
			logger.error("Params `nickname` is null.");
			return new ErrorResponse.Builder(-1, "params error!").build();
		}
		QueryParams queryParams = new QueryParams();
		queryParams.setQ(request.getParameter("q") == null ? "*:*" : request.getParameter("q"));
		queryParams.setFq(request.getParameter("fq") == null ? "" : request.getParameter("fq"));
		queryParams.setSort(request.getParameter("sort") == null ? "" : request.getParameter("sort"));
		queryParams.setStart(request.getParameter("start") == null ? 0
				: Integer.parseInt(request.getParameter("start")));
		queryParams.setRows(request.getParameter("rows") == null ? 10 : Integer.parseInt(request.getParameter("rows")));
		queryParams.setFl(request.getParameter("fl") == null ? "" : request.getParameter("fl"));
		queryParams.setQop("OR");
		logger.info(queryParams.toString());
		int type = request.getParameter("type") == null ? 0 : Integer.parseInt(request.getParameter("type"));
		if (type == 1) {
			return queryService.getRelatedData(queryParams, nickname);
		} else if (type == 2) {
			queryParams.setSort("sum(comment_count,repost_count):desc," + queryParams.getSort());
			QueryResult result = queryService.queryData(queryParams, nickname);
			result.setNumFound(result.getNumFound() > 200 ? 200 : result.getNumFound());
			return result;
		}
		return queryService.queryData(queryParams, nickname);
	}

	@RequestMapping(value = "/relation", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody Object relationAnalysed(HttpServletRequest request) {
		String nickname = request.getParameter("trueUser");
		if (nickname == null || nickname.isEmpty()) {
			logger.error("Params `nickname` is null.");
			return new ErrorResponse.Builder(-1, "params error!").build();
		}
		RelationRequest relationRequest = new RelationRequest();
		relationRequest.setService(EndPoint.RELATION);
		relationRequest.setTrueUserId(nickname);
		if (request.getParameter("fq") != null) {
			for (String fq : request.getParameter("fq").split(";")) {
				if (fq.contains("timestamp")) {
					relationRequest.setTimestamp(fq);
				}
				if (fq.contains("platform")) {
					relationRequest.setPlatform(Integer.parseInt(fq.split(":")[1]));
				}
				if (fq.contains("source_id")) {
					relationRequest.setSource_id(Integer.parseInt(fq.split(":")[1]));
				}
			}
		}
		logger.info(JsonUtils.toJsonWithoutPretty(relationRequest));
		return relationService.relationAnalysed(relationRequest);
	}

	@RequestMapping(value = "/relation/query", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody Object relationQuery(@RequestBody RelationRequest request) {
		logger.info(JsonUtils.toJsonWithoutPretty(request));
		if (request.getService().equals(EndPoint.POST)) {
			if (Strings.isNullOrEmpty(request.getTrueUserId())) {
				logger.error("Params `nickname` is null.");
				return new ErrorResponse.Builder(-1, "params error!").build();
			} else {
				return relationService.getRelationPosts(request);
			}
		}
		if (request.getService().equals(EndPoint.FOLLOWS)) {
			if (Strings.isNullOrEmpty(request.getTrueUserId()) || request.getVirtuals().isEmpty()) {
				logger.error("Params `nickname` is null.");
				return new ErrorResponse.Builder(-1, "params error!").build();
			} else {
				return relationService.getFollowsDetail(request);
			}
		}
		if (request.getService().equals(EndPoint.DETAIL) && Strings.isNullOrEmpty(request.getSolr_id())) {
			logger.error("Params `solr_id` is null.");
			return new ErrorResponse.Builder(-1, "params error!").build();
		}
		return relationService.getPostDetail(request);
	}

}
