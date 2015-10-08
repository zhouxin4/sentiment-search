package zx.soft.sent.incompa.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import zx.soft.sent.common.insight.AreaCode;
import zx.soft.sent.common.insight.TrueUserHelper;
import zx.soft.sent.common.insight.UserDomain;
import zx.soft.sent.common.insight.Virtuals.Virtual;

@Service
public class VirtualFilterService {

	public Object getVirtuals(int source_id) {
		List<Virtual> filters = new ArrayList<>();
		for (AreaCode area : AreaCode.values()) {
			String areaCode = area.getAreaCode();
			List<UserDomain> trueUsers = TrueUserHelper.getTrueUsers(areaCode);
			for (UserDomain user : trueUsers) {
				String trueUserId = user.getTureUserId();
				List<Virtual> virtuals = TrueUserHelper.getVirtuals(trueUserId);
				for (Virtual virtual : virtuals) {
					if (virtual.getSource_id() == source_id) {
						filters.add(virtual);
					}
				}
			}
		}
		return filters;
	}

}
