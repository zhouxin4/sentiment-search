package zx.soft.sent.spring.controller;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import zx.soft.sent.spring.domain.ErrorResponse;
import zx.soft.sent.spring.domain.TokenUpdateData;
import zx.soft.sent.spring.service.IndexService;

/**
 * 分词器token
 * @author donglei
 *  192.168.32.11 :8912
 *
 */
@Controller
@RequestMapping("/term")
public class TokenController {

	@Inject
	private IndexService indexService;

	@RequestMapping(value = "/add", method = RequestMethod.POST, headers = { "Content-type=application/json" })
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody ErrorResponse add(@RequestBody TokenUpdateData tokenData) {
		return indexService.insertNewTokens(tokenData);
	}
}
