package zx.soft.sent.insight.controller;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import zx.soft.sent.insight.domain.ErrorResponse;
import zx.soft.sent.insight.service.InsightService;
import zx.soft.sent.solr.domain.QueryParams;
import zx.soft.utils.log.LogbackUtil;
import zx.soft.utils.time.TimeUtils;

import com.google.common.base.Preconditions;

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
	private InsightService indexService;

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
		long sys = System.currentTimeMillis();
		long startTime = TimeUtils.getMidnight(sys, -6);
		queryParams.setFacetRangeStart(request.getParameter("facetRangeStart") == null ? TimeUtils
				.transToSolrDateStr(startTime) : request.getParameter("facetRangeStart"));
		queryParams.setFacetRangeEnd(request.getParameter("facetRangeEnd") == null ? TimeUtils.transToSolrDateStr(sys)
				: request.getParameter("facetRangeEnd"));
		queryParams.setFacetRangeGap(request.getParameter("facetRangeGap") == null ? "+1HOUR" : request
				.getParameter("facetRangeGap"));

		logger.info(queryParams.toString());
		return indexService.getNicknamePostInfos(queryParams, nickname);
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
		queryParams.setRows(0);
		queryParams.setQop("OR");
		logger.info(queryParams.toString());
		return indexService.getTrendInfos(queryParams, nickname);
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
			return indexService.getRelatedData(queryParams, nickname);
		}
		return indexService.queryData(queryParams, nickname);
	}

	@RequestMapping(value = "/relation", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody Object relationAnalysed(HttpServletRequest request) {
		String nickname = request.getParameter("trueUser");
		if (nickname == null || nickname.isEmpty()) {
			logger.error("Params `nickname` is null.");
			return new ErrorResponse.Builder(-1, "params error!").build();
		}
		QueryParams queryParams = new QueryParams();
		queryParams.setQ(request.getParameter("q") == null ? "*:*" : request.getParameter("q"));
		queryParams.setFq(request.getParameter("fq") == null ? "" : request.getParameter("fq"));
		queryParams.setQop("OR");
		logger.info(queryParams.toString());
		return indexService.relationAnalysed(queryParams, nickname);
	}

}
