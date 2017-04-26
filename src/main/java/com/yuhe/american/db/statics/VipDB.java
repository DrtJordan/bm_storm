package com.yuhe.american.db.statics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.yuhe.american.db.DBManager;

public class VipDB {
	/**
	 * 记录VIP人数和V当前激活状态下的VIP人数
	 * 
	 * @param platformID
	 * @param hostID
	 * @param date
	 * @param vipLevel
	 * @param vipNumMap
	 * @return
	 */
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
			int uidNum = 0;
			if (vipNumMap.containsKey(col)) {
				uidNum = vipNumMap.get(col).size();
			}
			valueList.add(Integer.toString(uidNum));
			updateList.add(col + "='" + uidNum + "'");
		}
		String sql = sb.append(StringUtils.join(valueList, "','")).append("') on duplicate key update ")
				.append(StringUtils.join(updateList, ",")).toString();
		DBManager.execute(sql);
		return true;
	}

	/**
	 * 记录VIP等级的流失人数
	 * 
	 * @param platformID
	 * @param hostID
	 * @param date
	 * @param vipLevel
	 * @param lostNum
	 * @return
	 */
	public static boolean updateLostNum(String platformID, String hostID, String date,
			Map<String, Integer> lostNumMap) {
		List<String> sqlList = new ArrayList<String>();
		Iterator<String> it = lostNumMap.keySet().iterator();
		String headSql = "update " + platformID + "_statics.tblVIP set LostNum = '";
		String where = "' where HostID = '" + hostID + "' and Date = '" + date;
		while (it.hasNext()) {
			String vipLevel = it.next();
			int lostNum = lostNumMap.get(vipLevel);
			String sql = headSql + lostNum + where + "' and VipLevel = '" + vipLevel + "';";
			sqlList.add(sql);
		}
		if (sqlList.size() > 0)
			DBManager.execute(StringUtils.join(sqlList, ""));
		return true;
	}
}
