package zx.soft.sent.origin.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.ansj.app.keyword.KeyWordComputer;
import org.ansj.app.keyword.Keyword;
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
import zx.soft.sent.origin.service.OriginService;
import zx.soft.utils.log.LogbackUtil;
import zx.soft.utils.string.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;

/**
 * 索引控制类
 *
 * @author wanggang
 *
 */
@Controller
@RequestMapping("/origin")
public class OriginController {

	private static Logger logger = LoggerFactory.getLogger(OriginController.class);

	private static KeyWordComputer kwc = new KeyWordComputer(6);

	@Inject
	private OriginService originService;

	@RequestMapping(value = "/keywords", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody Object extractKeyword(@RequestBody String text) {
		if (StringUtils.isEmpty(text)) {
			return new ArrayList<String>();
		}
		Collection<Keyword> keywords = kwc.computeArticleTfidf(text);
		Collection<String> keys = Collections2.transform(keywords, new Function<Keyword, String>() {

			@Override
			public String apply(Keyword input) {
				// TODO Auto-generated method stub
				return input.getName();
			}
		});
		return keys;
	}

	@RequestMapping(value = "/counts", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody Object countNum(@RequestBody List<String> mds) {
		if (mds.isEmpty()) {
			return new HashMap<String, Map<String, Integer>>();
		}
		return originService.getCounts(mds);
	}

	@RequestMapping(value = "/posts", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody Object getOriginPosts(HttpServletRequest request) {
		String key = request.getParameter("key");
		try {
			Preconditions.checkArgument(key != null && !key.isEmpty(), "Params `key` is null.");
		} catch (IllegalArgumentException e) {
			logger.error("Exception: {}", LogbackUtil.expection2Str(e));
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
		queryParams.setFq(queryParams.getFq() + ";cache_type:1;cache_id:" + key);
		logger.info(queryParams.toString());
		return originService.getOriginPosts(queryParams);
	}

	@RequestMapping(value = "/trend", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody Object getPostsResult(HttpServletRequest request) {
		String key = request.getParameter("key");
		try {
			Preconditions.checkArgument(key != null && !key.isEmpty(), "Params `key` is null.");
		} catch (IllegalArgumentException e) {
			logger.error("Exception: {}", LogbackUtil.expection2Str(e));
			return new ErrorResponse.Builder(-1, "params error!").build();
		}

		return originService.getOriginTrends(key);
	}

	@RequestMapping(value = "/negative", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody Object getNegativeResult(HttpServletRequest request) {
		QueryParams queryParams = new QueryParams();
		queryParams.setQ(request.getParameter("q") == null ? "*:*" : request.getParameter("q"));
		queryParams.setFq(request.getParameter("fq") == null ? "" : request.getParameter("fq"));
		queryParams.setSort(request.getParameter("sort") == null ? "" : request.getParameter("sort"));
		queryParams.setStart(request.getParameter("start") == null ? 0
				: Integer.parseInt(request.getParameter("start")));
		queryParams.setRows(request.getParameter("rows") == null ? 10 : Integer.parseInt(request.getParameter("rows")));
		queryParams.setQop("OR");
		queryParams.setFq(queryParams.getFq() + ";cache_type:2");
		queryParams.setFl(request.getParameter("fl") == null ? "" : request.getParameter("fl"));
		queryParams.setFacetField(request.getParameter("facetField") == null ? "" : request.getParameter("facetField"));
		logger.info(queryParams.toString());
		return originService.getOriginPosts(queryParams);
	}

}
