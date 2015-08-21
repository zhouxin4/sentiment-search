package zx.soft.sent.origin.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import zx.soft.sent.core.domain.ErrorResponse;
import zx.soft.sent.origin.service.OriginService;
import zx.soft.utils.log.LogbackUtil;
import zx.soft.utils.string.StringUtils;

import com.google.common.base.Preconditions;
import com.hankcs.hanlp.HanLP;

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

	@Inject
	private OriginService originService;

	@RequestMapping(value = "/keywords", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody Object extractKeyword(@RequestBody String text) {
		if (StringUtils.isEmpty(text)) {
			return new ArrayList<String>();
		}
		return HanLP.extractKeyword(text, 50);
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
		int start = Integer.parseInt(request.getParameter("start") == null ? "0" : request.getParameter("start"));
		int rows = Integer.parseInt(request.getParameter("rows") == null ? "10" : request.getParameter("rows"));
		return originService.getOriginPosts(key, start, rows);
	}

}
