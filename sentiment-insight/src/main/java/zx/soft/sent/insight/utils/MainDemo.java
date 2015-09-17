package zx.soft.sent.insight.utils;

import zx.soft.sent.common.insight.HbaseConstant;
import zx.soft.sent.insight.domain.RelationRequest;
import zx.soft.sent.insight.domain.RelationRequest.EndPoint;
import zx.soft.utils.string.StringUtils;
import zx.soft.utils.time.TimeUtils;

public class MainDemo {
	public static void main(String[] args) {
		final RelationRequest request = new RelationRequest();
		request.setService(EndPoint.POST);
		request.setTimestamp("[2015-09-01T16:43:18Z TO 2015-09-01T16:43:30Z]");
		request.setSource_id(7);
		request.setPlatform(3);
		new Thread(new Runnable() {

			@Override
			public void run() {
				String sql = generateCONSQL(request);
				System.out.println(sql);
			}
		}).start();
		new Thread(new Runnable() {

			@Override
			public void run() {
				String sql = generateCONSQL(request);
				System.out.println(sql);
			}
		}).start();
		//		System.out.println(generateCONSQL(request));

	}

	private static String generateCONSQL(RelationRequest request) {

		StringBuilder sBuilder = new StringBuilder();
		switch (request.getService()) {
		case POST:
			if (!StringUtils.isEmpty(request.getQ())) {
				sBuilder.append(HbaseConstant.TEXT + " RLIKE " + "'.*" + request.getQ() + ".*'");
			}
		case RELATION:
			if (request.getTrueUserId() != null) {
				if (sBuilder.length() > 0) {
					sBuilder.append(" AND ");
				}
				sBuilder.append(HbaseConstant.TRUE_USER + "='" + request.getTrueUserId() + "'");
			}
			if (request.getPlatform() != -1) {
				sBuilder.append(" AND pl=" + request.getPlatform());
			}
			if (request.getSource_id() != -1) {
				sBuilder.append(" AND sid=" + request.getSource_id());
			}
			String timestamp = request.getTimestamp();
			if (timestamp != null) {
				long endTime = System.currentTimeMillis();
				long startTime = TimeUtils.transCurrentTime(endTime, 0, 0, -6, 0);
				int li = timestamp.indexOf("[");
				int ri = timestamp.indexOf("TO");
				int lf = timestamp.indexOf("]");
				try {
					startTime = TimeUtils.tranSolrDateStrToMilli(timestamp.substring(li + 1, ri).trim());
					endTime = TimeUtils.tranSolrDateStrToMilli(timestamp.substring(ri + 2, lf).trim());
				} catch (Exception e) {
					e.printStackTrace();
				}
				sBuilder.append(" AND ts BETWEEN " + startTime + " AND " + endTime);
			}
			if (!request.getVirtuals().isEmpty()) {
				sBuilder.append(" AND " + HbaseConstant.COMMENT_USER + " IN (");
				for (String vir : request.getVirtuals()) {
					sBuilder.append("'" + vir + "',");
				}
				sBuilder.setLength(sBuilder.length() - 1);
				sBuilder.append(")");
			}
			break;
		case DETAIL:
			if (request.getSolr_id() != null) {
				sBuilder.append(" id='" + request.getSolr_id() + "'");
			}
			if (!request.getVirtuals().isEmpty()) {
				sBuilder.append(" AND " + HbaseConstant.COMMENT_USER + " IN (");
				for (String vir : request.getVirtuals()) {
					sBuilder.append("'" + vir + "',");
				}
				sBuilder.setLength(sBuilder.length() - 1);
				sBuilder.append(")");
			}
			break;

		}
		;

		return sBuilder.toString();
	}
}
