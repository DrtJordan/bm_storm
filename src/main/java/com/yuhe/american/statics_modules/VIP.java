package com.yuhe.american.statics_modules;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.yuhe.american.db.DBManager;
import com.yuhe.american.db.log.CommonDB;
import com.yuhe.american.db.statics.VipDB;
import com.yuhe.american.utils.DateUtils2;

public class VIP extends AbstractStaticsModule {
	// 记录今天登陆过的VIP等级人数。格式:Map<PlatformID,Map<HostID, Map<Date,Map<VipLevel,
	// Map<Type,
	// Set<Uid>>>>>>
	private Map<String, Map<String, Map<String, Map<String, Map<String, Set<String>>>>>> VipResultsMap = new HashMap<String, Map<String, Map<String, Map<String, Map<String, Set<String>>>>>>();
	// 记录前后8天之内登陆过的VIP玩家的uid，格式：Map<PlatformID,Map<HostID, Map<Date,Set<Uid>>>>
	private Map<String, Map<String, Map<String, Set<String>>>> LoginVipMap = new HashMap<String, Map<String, Map<String, Set<String>>>>();
	//统计标志位，定时统计前先判断一下是否需要统计,格式:Map<HostID,Map<Date, Boolean>>
	private Map<String, Map<String, Map<String, Boolean>>> StaticsFlagMap = new HashMap<String, Map<String, Map<String, Boolean>>>();
	@Override
	public boolean execute(Map<String, List<Map<String, String>>> platformResults) {
		for (String platformID : platformResults.keySet()) {
			List<Map<String, String>> platformResult = platformResults.get(platformID);
			for (Map<String, String> logMap : platformResult) {
				if (logMap.containsKey("VipLevel") && !logMap.get("VipLevel").equals("0")) {
					updateVipResultsMap(platformID, logMap);
					updateLoginVipMap(platformID, logMap);
				}
			}
		}
		return true;
	}

	/**
	 * 更新VipResultsMap
	 * 
	 * @param platformID
	 * @param logMap
	 */
	private void updateVipResultsMap(String platformID, Map<String, String> logMap) {
		String hostID = logMap.get("HostID");
		String vipLevel = logMap.get("VipLevel");
		String uid = logMap.get("Uid");
		String isVip = logMap.get("IsVip");
		String time = logMap.get("Time");
		String[] times = StringUtils.split(time, " ");
		String date = times[0];
		Map<String, Map<String, Map<String, Map<String, Set<String>>>>> pResult = VipResultsMap.get(platformID);
		if (pResult == null) {
			pResult = new HashMap<String, Map<String, Map<String, Map<String, Set<String>>>>>();
			VipResultsMap.put(platformID, pResult);
		}
		Map<String, Map<String, Map<String, Set<String>>>> hostResult = pResult.get(hostID);
		if (hostResult == null) {
			hostResult = new HashMap<String, Map<String, Map<String, Set<String>>>>();
			Map<String, Map<String, Set<String>>> dateResult = loadVipNumFromDB(platformID, hostID, date);
			hostResult.put(date, dateResult);
			pResult.put(hostID, hostResult);
		}
		Map<String, Map<String, Set<String>>> dateResult = hostResult.get(date);
		if (dateResult == null) {
			// 同时要清除其他天数的VIP记录
			hostResult = new HashMap<String, Map<String, Map<String, Set<String>>>>();
			pResult.put(hostID, hostResult);
			dateResult = new HashMap<String, Map<String, Set<String>>>();
			hostResult.put(date, dateResult);
		}
		Map<String, Set<String>> levelResult = dateResult.get(vipLevel);
		if (levelResult == null) {
			levelResult = new HashMap<String, Set<String>>();
			levelResult.put("VipNums", new HashSet<String>());
			levelResult.put("NowVipNums", new HashSet<String>());
			dateResult.put(vipLevel, levelResult);
		}
		levelResult.get("VipNums").add(uid);
		if (isVip.equals("1")) {
			levelResult.get("NowVipNums").add(uid);
		}
	}

	/**
	 * 更新LoginVipMap
	 * 
	 * @param platformID
	 * @param logMap
	 */
	private void updateLoginVipMap(String platformID, Map<String, String> logMap) {
		String hostID = logMap.get("HostID");
		String uid = logMap.get("Uid");
		String time = logMap.get("Time");
		String[] times = StringUtils.split(time, " ");
		String date = times[0];
		Map<String, Map<String, Set<String>>> pResult = LoginVipMap.get(platformID);
		if (pResult == null) {
			pResult = new HashMap<String, Map<String, Set<String>>>();
			LoginVipMap.put(platformID, pResult);
		}
		Map<String, Set<String>> hResult = pResult.get(hostID);
		if (hResult == null) {
			hResult = loadVipUidFromDB(platformID, hostID, date);
			pResult.put(hostID, hResult);
		}
		Set<String> dResult = hResult.get(date);
		if (dResult == null) {
			dResult = new HashSet<String>();
			hResult.put(date, dResult);
			// 将8天之前的数据清空
			String off9Day = DateUtils2.getOverDate(date, -9);
			hResult.remove(off9Day);
		}
		int orgSize = dResult.size();
		dResult.add(uid);
		int nowSize = dResult.size();
		if(nowSize > orgSize){
			//人数有变化，需要更新统计标志位
			updateStaticsFlag(platformID, hostID, date, true);
		}
	}
	/**
	 * 更新统计标志位
	 * @param hostID
	 * @param date
	 * @param flag
	 */
	private void updateStaticsFlag(String platformID, String hostID, String date, boolean flag){
		Map<String, Map<String, Boolean>> pFlagMap = StaticsFlagMap.get(platformID);
		if(pFlagMap == null){
			pFlagMap = new HashMap<String, Map<String, Boolean>>();
			StaticsFlagMap.put(platformID, pFlagMap);
		}
		Map<String, Boolean> hostFlagMap = pFlagMap.get(hostID);
		if(hostFlagMap == null){
			hostFlagMap = new HashMap<String, Boolean>();
			pFlagMap.put(hostID, hostFlagMap);
		}
		hostFlagMap.put(date, flag);
	}

	/**
	 * 从tblLogoutLog表中加载当天各VIP等级的玩家uid列表
	 * 
	 * @param platformID
	 * @param hostID
	 * @param date
	 * @return
	 */
	private Map<String, Map<String, Set<String>>> loadVipNumFromDB(String platformID, String hostID, String date) {
		Map<String, Map<String, Set<String>>> vipNumMap = new HashMap<String, Map<String, Set<String>>>();
		String tblName = platformID + "_log.tblLogoutLog_" + date.replace("-", "");
		List<String> options = new ArrayList<String>();
		options.add("HostID = '" + hostID + "'");
		options.add("Time >= '" + date + " 00:00:00'");
		options.add("Time < '" + date + " 23:59:59'");
		options.add("VipLevel != '0'");
		Connection conn = DBManager.getConn();
		try {
			Statement smst = conn.createStatement();
			ResultSet resultSet = CommonDB.query(smst, conn, tblName, options);
			while (resultSet.next()) {
				String vipLevel = resultSet.getString("VipLevel");
				int isVip = resultSet.getInt("IsVip");
				String uid = resultSet.getString("Uid");
				Map<String, Set<String>> levelResult = vipNumMap.get(vipLevel);
				if (levelResult == null) {
					levelResult = new HashMap<String, Set<String>>();
					levelResult.put("VipNums", new HashSet<String>());
					levelResult.put("NowVipNums", new HashSet<String>());
					vipNumMap.put(vipLevel, levelResult);
				}
				if (isVip == 1) {
					Set<String> nowVipUids = levelResult.get("NowVipNums");
					nowVipUids.add(uid);
				}
				Set<String> vipNumUids = levelResult.get("VipNums");
				vipNumUids.add(uid);
			}
			resultSet.close();
			smst.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			DBManager.closeConn(conn);
		}
		return vipNumMap;
	}

	/**
	 * 从tblLogoutLog表中加载八天内登陆的VIP玩家uid
	 * 
	 * @param platformID
	 * @param hostID
	 * @param date
	 * @return
	 */
	private Map<String, Set<String>> loadVipUidFromDB(String platformID, String hostID, String date) {
		Map<String, Set<String>> uidMap = new HashMap<String, Set<String>>();
		int[] offDays = { -8, -7, -6, -5, -4, -3, -2, -1, 0 };
		List<String> sqlList = new ArrayList<String>();
		String options = " where HostID = '" + hostID + "' and VipLevel != '0'";
		for (int offDay : offDays) {
			String day = DateUtils2.getOverDate(date, offDay);
			String tblName = platformID + "_log.tblLogoutLog_" + day.replace("-", "");
			String tSql = "select * from " + tblName + options;
			sqlList.add(tSql);
		}
		String sql = StringUtils.join(sqlList, " union ");
		Connection conn = DBManager.getConn();
		
		try {
			Statement smst = conn.createStatement();
			ResultSet resultSet = DBManager.query(smst, conn, sql);
			while (resultSet.next()) {
				String uid = resultSet.getString("Uid");
				String time = resultSet.getString("Time");
				String[] times = StringUtils.split(time, " ");
				String tDate = times[0];
				Set<String> dateUids = uidMap.get(tDate);
				if (dateUids == null) {
					dateUids = new HashSet<String>();
					uidMap.put(tDate, dateUids);
				}
				dateUids.add(uid);
			}
			resultSet.close();
			smst.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			DBManager.closeConn(conn);
		}
		return uidMap;
	}

	@Override
	public boolean cronExecute() {
		staticsVipResultsMap();
		checkFlagMap();
		return true;
	}
	/**
	 * 将VipResultsMap统计入库
	 */
	private void staticsVipResultsMap() {
		synchronized (VipResultsMap) {
			Iterator<String> pIt = VipResultsMap.keySet().iterator();
			while (pIt.hasNext()) {
				String platformID = pIt.next();
				Map<String, Map<String, Map<String, Map<String, Set<String>>>>> pResult = VipResultsMap.get(platformID);
				Iterator<String> hIt = pResult.keySet().iterator();
				while (hIt.hasNext()) {
					String hostID = hIt.next();
					Map<String, Map<String, Map<String, Set<String>>>> hostResult = pResult.get(hostID);
					Iterator<String> dIt = hostResult.keySet().iterator();
					while (dIt.hasNext()) {
						String date = dIt.next();
						Map<String, Map<String, Set<String>>> dateResult = hostResult.get(date);
						Iterator<String> lIt = dateResult.keySet().iterator();
						while (lIt.hasNext()) {
							String vipLevel = lIt.next();
							Map<String, Set<String>> levelResult = dateResult.get(vipLevel);
							VipDB.insert(platformID, hostID, date, vipLevel, levelResult);
						}
						if (dateResult.size() > 0) {
							// dateResult置为空
							dateResult = new HashMap<String, Map<String, Set<String>>>();
							hostResult.put(date, dateResult);
						}
					}
				}
			}
		}
	}
	
	private void checkFlagMap(){
		synchronized(StaticsFlagMap){
			Iterator<String> pIt = StaticsFlagMap.keySet().iterator();
			while(pIt.hasNext()){
				String platformID = pIt.next();
				Map<String, Map<String, Boolean>> pFlagMap = StaticsFlagMap.get(platformID);
				Iterator<String> hIt = pFlagMap.keySet().iterator();
				while(hIt.hasNext()){
					String hostID = hIt.next();
					Map<String, Boolean> hostFlagMap = pFlagMap.get(hostID);
					Iterator<String> dIt = hostFlagMap.keySet().iterator();
					while(dIt.hasNext()){
						String date = dIt.next();
						boolean flag = hostFlagMap.get(date);
						if(flag==true){
							staticsVipLostNum(platformID, hostID, date);
							hostFlagMap.put(date, false);
						}
					}
				}
			}
			
		}
	}
	/**
	 * 统计VIP流失率,流失率计算规则为连续7天都没有登录过游戏即为流失
	 * 
	 * @param platform
	 * @param hostID
	 * @param date
	 */
	private void staticsVipLostNum(String platformID, String hostID, String date){
		if(LoginVipMap.containsKey(platformID) && LoginVipMap.get(platformID).containsKey(hostID)){
			Map<String, Set<String>> dateNumMap = LoginVipMap.get(platformID).get(hostID);
			String off8Day = DateUtils2.getOverDate(date, -8);
			Set<String> login8Set = dateNumMap.get(off8Day);
			Set<String> loginSet = new HashSet<String>();
			int[] offs = {-7, -6, -5, -4, -3, -2, -1, 0};
			for(int off: offs){
				String offDay = DateUtils2.getOverDate(date, off);
				Set<String> tLoginSet = dateNumMap.get(offDay);
				loginSet.addAll(tLoginSet);
			}
			int lostNum = 0;
			for(String uid: login8Set){
				if(!loginSet.contains(uid)){
					lostNum++;
				}
			}
			float rate = 0;
			float bench = 100;
			rate = Math.round(lostNum * 10000 / login8Set.size()) / bench;
			//TODO:未完成版本，没有纳入实时统计，后面有时间再改吧
		}
		
	}

}
