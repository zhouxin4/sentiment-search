package zx.soft.sent.common.insight;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.common.insight.CommonRequest.KeyUnit;
import zx.soft.sent.common.insight.CommonRequest.Unit;
import zx.soft.sent.common.insight.Virtuals.Virtual;
import zx.soft.utils.http.HttpClientDaoImpl;
import zx.soft.utils.json.JsonNodeUtils;
import zx.soft.utils.json.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Predicate;

public class TrueUserHelper {
	private static Logger logger = LoggerFactory.getLogger(TrueUserHelper.class);

	public static List<UserDomain> getTrueUsers(String areaCode) {
		List<UserDomain> users = new ArrayList<>();
		HttpClientDaoImpl httpclient = new HttpClientDaoImpl();
		// 所有重点人员
		boolean[] types = { true, false };
		// 省厅添加的用户
		//		boolean[] types = { true };

		for (boolean type : types) {
			String data = "{" + "\"areaCode\":" + areaCode + "," + "\"user\":" + type + "}";
			String response = httpclient.doPostAndGetResponse(InsightConstant.TRUE_USER, data);
			if (!"error".equals(response)) {
				JsonNode node = JsonNodeUtils.getJsonNode(response, "response");
				List<UserDomain> virs = JsonUtils.parseJsonArray(node.toString(), UserDomain.class);
				users.addAll(virs);
			}
		}

		return users;
	}

	public static UserDomain getUserInfo(String trueUserId) {
		Virtuals virtuals = VirtualsCache.getVirtuals(trueUserId);
		if (virtuals != null && virtuals.getTrueUser() != null) {
			return virtuals.getTrueUser();
		}
		HttpClientDaoImpl httpClient = new HttpClientDaoImpl();
		String response = httpClient.doGet(InsightConstant.TRUE_USER + trueUserId);
		JsonNode node = JsonNodeUtils.getJsonNode(response, "response");
		List<UserDomain> users = JsonUtils.parseJsonArray(node.toString(), UserDomain.class);
		if (users.isEmpty()) {
			return null;
		} else {
			VirtualsCache.addTrueUser(trueUserId, users.get(0));
			return users.get(0);
		}
	}

	public static List<Virtual> getVirtuals(String trueUser) {
		Virtuals virtuals = VirtualsCache.getVirtuals(trueUser);
		if (virtuals != null && !virtuals.getVirtuals().isEmpty()) {
			return virtuals.getVirtuals();
		}
		String data = "{\"trueUserId\":\"" + trueUser + "\",\"page\":1,\"size\":100}";
		HttpClientDaoImpl httpclient = new HttpClientDaoImpl();
		long start = System.currentTimeMillis();
		String response = httpclient.doPostAndGetResponse(InsightConstant.VIRTUAL_URL, data);
		logger.info("获取虚拟账号耗时: {}", System.currentTimeMillis() - start);
		if (!"error".equals(response)) {
			JsonNode node = JsonNodeUtils.getJsonNode(response, "response");
			List<Virtual> virs = JsonUtils.parseJsonArray(node.toString(), Virtual.class);
			if (!virs.isEmpty()) {
				VirtualsCache.addVirtuals(trueUser, virs);
			}
			return virs;
		}
		return new ArrayList<>();
	}

	public static List<Group> getTrendGroup(boolean isUser) {
		List<Group> groups = new ArrayList<>();
		long start = System.currentTimeMillis();
		CommonRequest request = new CommonRequest();
		request.setType("unit");
		request.setOperation(3);
		request.setKeyUnit(new KeyUnit(new Unit(340000, isUser)));
		HttpClientDaoImpl httpclient = new HttpClientDaoImpl();
		String response = httpclient.doPostAndGetResponse(InsightConstant.GROUP_URL,
				JsonUtils.toJsonWithoutPretty(request));
		if (!"error".equals(response)) {
			JsonNode node = JsonNodeUtils.getJsonNode(response, "response");
			List<Group> group = JsonUtils.parseJsonArray(node.toString(), Group.class);
			groups.addAll(group);
		}
		logger.info("获取关键词分组耗时: {}", System.currentTimeMillis() - start);
		return groups;
	}

	public static class VirtualPredicate implements Predicate<Virtual> {

		private int platform = -1;
		private int source_id = -1;

		public VirtualPredicate(String fq) {
			if (fq.contains("source_id") || fq.contains("platform")) {
				for (String sp : fq.split(";")) {
					if (sp.contains("source_id")) {
						source_id = Integer.parseInt(sp.split(":")[1]);
					}
					if (sp.contains("platform")) {
						platform = Integer.parseInt(sp.split(":")[1]);
					}
				}
			}
		}

		@Override
		public boolean apply(Virtual virtual) {
			if (platform != -1 && platform != virtual.getPlatform()) {
				return false;
			}
			if (source_id != -1 && source_id != virtual.getSource_id()) {
				return false;
			}
			return true;
		}

	}

}
