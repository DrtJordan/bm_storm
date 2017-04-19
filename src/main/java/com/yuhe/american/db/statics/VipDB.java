package com.yuhe.american.db.statics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

public class VipDB {

	public static boolean insert(String platformID, String hostID, String date, String vipLevel,
			Map<String, Set<String>> vipNumMap) {
		String[] vipCols = { "VipNum", "NowVipNum" };
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(platformID).append("_statics.tblVIP(PlatformID, HostID, Date, VipLevel, ")
				.append(StringUtils.join(vipCols, ",")).append(") values('").append(platformID).append("','")
				.append(hostID).append("','").append(date).append("','").append(vipLevel).append("','");
		List<String> valueList = new ArrayList<String>();
		List<String> updateList = new ArrayList<String>();
		for (String col : vipCols) {
			int uidNum = vipNumMap.get(col).size();
			valueList.add(Integer.toString(uidNum));
			updateList.add(col + "='" + uidNum + "'");
		}
		sb.append(StringUtils.join(valueList, "','")).append("') on duplicate key update ")
				.append(StringUtils.join(updateList, ","));
		return true;
	}
}
