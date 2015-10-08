package zx.soft.sent.incompa.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import zx.soft.sent.incompa.service.VirtualFilterService;

@RestController
public class UserController {

	@Autowired
	private VirtualFilterService virtualService;

	@RequestMapping("/create")
	@ResponseBody
	public String create(String email, String firstName, String lastName) {
		return "";
	}

	@RequestMapping("/virtual/{source_id}")
	public Object getVirtuals(@PathVariable int source_id) {
		return virtualService.getVirtuals(source_id);
	}
}