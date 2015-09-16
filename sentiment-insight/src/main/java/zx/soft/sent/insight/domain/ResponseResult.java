package zx.soft.sent.insight.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 回复分页显示内容
 * @author donglei
 */
public class ResponseResult {
	private long numFound;
	private List<BlogResponse> responses = new ArrayList<>();

	public void addResponse(BlogResponse res) {
		responses.add(res);
	}

	public long getNumFound() {
		return numFound;
	}

	public void setNumFound(long numFound) {
		this.numFound = numFound;
	}

	public List<BlogResponse> getResponses() {
		return responses;
	}

	public void setResponses(List<BlogResponse> responses) {
		this.responses = responses;
	}

}
