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
import zx.soft.utils.time.TimeUtils;

/**
 * @author donglei
 */
@Controller
@RequestMapping("/insights")
public class InsightController {
	Logger logger = LoggerFactory.getLogger(InsightController.class);

	@Inject
	private InsightService indexService;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody Object getUserInfo(HttpServletRequest request) {
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
				.transToSolrDateStr(startTime) : request
				.getParameter("facetRangeStart"));
		queryParams.setFacetRangeEnd(request.getParameter("facetRangeEnd") == null ? TimeUtils
.transToSolrDateStr(sys)
				: request
				.getParameter("facetRangeEnd"));
		queryParams.setFacetRangeGap(request.getParameter("facetRangeGap") == null ? "+1HOUR" : request
				.getParameter("facetRangeGap"));


		logger.info(queryParams.toString());
		return indexService.getNicknamePostInfos(queryParams, nickname);
	}

}
