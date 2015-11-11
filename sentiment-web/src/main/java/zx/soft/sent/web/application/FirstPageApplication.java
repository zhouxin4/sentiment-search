package zx.soft.sent.web.application;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.solr.common.SolrDocument;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import zx.soft.sent.dao.firstpage.FirstPagePersistable;
import zx.soft.sent.dao.firstpage.RiakFirstPage;
import zx.soft.sent.web.resource.FirstPageResource;
import zx.soft.utils.json.JsonNodeUtils;
import zx.soft.utils.json.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;

public class FirstPageApplication extends Application {

	//	private static Logger logger = LoggerFactory.getLogger(FirstPageApplication.class);

	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
	//	private static final SimpleDateFormat FORMATTER_HH = new SimpleDateFormat("yyyy-MM-dd,HH");

	private final FirstPagePersistable firstPage;

	public FirstPageApplication() {
		//		firstPage = new FirstPage(MybatisConfig.ServerEnum.sentiment);
		firstPage = new RiakFirstPage();
	}

	@Override
	public Restlet createInboundRoot() {
		Router router = new Router(getContext());
		// POST专题信息
		router.attach("/{type}/{datestr}", FirstPageResource.class);
		return router;
	}

	/**
	 * 查询OA首页查询数据
	 */
	public String selectFirstPage(int type, String timestr) {
		return firstPage.selectFirstPage(type, timestr);
	}

	/**
	 * 查询OA首页查询数据，type=1,2
	 */
	public HashMap<String, Long> selectFirstPageType12(int type, String timestr) {
		//		return trans2Map(firstPage.selectFirstPage(type, getCurrentHH()));
		return trans2Map(firstPage.selectFirstPage(type, timestr));
	}

	/**
	 * 查询OA首页查询数据，type=4
	 */
	public HashMap<String, HashMap<String, Long>> selectFirstPageType4(String datestr) {
		int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		HashMap<String, HashMap<String, Long>> result = new HashMap<>();
		HashMap<String, Long> temp = null;
		for (int i = 0; i <= hour; i = i + 3) {
			String timestr = getCurrentYearMonthDay();
			if (i < 10) {
				timestr += ",0" + i;
			} else {
				timestr += "," + i;
			}
			temp = trans2Map(firstPage.selectFirstPage(4, timestr));
			if (temp == null) {
				continue;
			}
			for (Entry<String, Long> t : temp.entrySet()) {
				if (result.get(t.getKey()) == null) {
					result.put(t.getKey(), new HashMap<String, Long>());
				}
				result.get(t.getKey()).put(i + "", t.getValue());
			}
		}
		if (result.size() == 0) {
			return null;
		} else {
			return result;
		}
	}

	/**
	 * 查询OA首页查询数据，type=52,53
	 */
	public List<SolrDocument> selectFirstPageType5(int type, String timestr) {
		List<SolrDocument> result = new ArrayList<>();
		String resultStr = firstPage.selectFirstPage(type, timestr);
		if (resultStr == null) {
			return null;
		}
		return JsonUtils.parseJsonArray(resultStr, SolrDocument.class);
	}

	private HashMap<String, Long> trans2Map(String jsonStr) {
		if (jsonStr == null) {
			return null;
		}
		HashMap<String, Long> result = new HashMap<>();
		JsonNode node = JsonNodeUtils.getJsonNode(jsonStr);
		Iterator<String> keys = node.fieldNames();
		String key = null;
		while (keys.hasNext()) {
			key = keys.next();
			result.put(key, Long.parseLong(node.get(key).toString()));
		}

		return result;
	}

	private String getCurrentYearMonthDay() {
		return FORMATTER.format(new Date());
	}

	//	private String getCurrentHH() {
	//		return FORMATTER_HH.format(new Date());
	//	}

	@Override
	public void stop() throws Exception {
		super.stop();
	}

}
