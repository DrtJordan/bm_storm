package com.yuhe.american.statics_modules;

import java.util.List;
import java.util.Map;

public class MixLoginStatics extends AbstractStaticsModule {

	@Override
	public boolean execute(Map<String, List<Map<String, String>>> platformResults) {
		Map<String, AbstractStaticsModule> indexMap = StaticsIndexes.GetIndexMap();
		AbstractStaticsModule retentionModule = indexMap.get("Retention");
		AbstractStaticsModule onlineTimeModule = indexMap.get("OnlineTime");
		AbstractStaticsModule vipModule = indexMap.get("VIP");
		retentionModule.execute(platformResults);
		onlineTimeModule.execute(platformResults);
		vipModule.execute(platformResults);
		return true;
	}

	@Override
	public boolean cronExecute() {
		// TODO Auto-generated method stub
		return false;
	}

}
