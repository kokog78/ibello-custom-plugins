package hu.ibello.plugins.jmeter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hu.ibello.plugins.jmeter.model.ConcurrentRequestData;
import hu.ibello.plugins.jmeter.model.GroupedRequestData;
import hu.ibello.plugins.jmeter.model.JmeterResult;

public class ResultStatCollector {

	private final List<JmeterResult> results;
	
	private ConcurrentRequestData totalConcurrentStats;
	private GroupedRequestData totalGroupedStats;
	
	public ResultStatCollector(List<JmeterResult> results) {
		this.results = results;
	}
	
	public ConcurrentRequestData getTotalConcurrentStats() {
		return totalConcurrentStats;
	}
	
	public GroupedRequestData getTotalGroupedStats() {
		return totalGroupedStats;
	}
	
	public List<ConcurrentRequestData> getConcurrentStats(int satisfiedThresholds, int toleratedThresholds) {
		totalConcurrentStats = new ConcurrentRequestData(satisfiedThresholds, toleratedThresholds);
		Map<String, ConcurrentRequestData> map = new HashMap<>();
		for (JmeterResult result : results) {
			ConcurrentRequestData requestData = getConcurrentRequestDataFor(map, result, satisfiedThresholds, toleratedThresholds);
			requestData.register(result);
			totalConcurrentStats.register(result);
		}
		List<ConcurrentRequestData> list = new ArrayList<>(map.values());
		Collections.sort(list, (data1, data2) -> data1.count() - data2.count());
		return list;
	}
	
	public List<GroupedRequestData> getGroupedStats() {
		totalGroupedStats = new GroupedRequestData(null);
		Map<String, GroupedRequestData> map = new HashMap<>();
		for (JmeterResult result : results) {
			GroupedRequestData requestData = getGroupedRequestDataFor(map, result);
			requestData.register(result);
			totalGroupedStats.register(result);
		}
		List<GroupedRequestData> list = new ArrayList<>(map.values());
		Collections.sort(list, (data1, data2) -> data1.getLabel().compareTo(data2.getLabel()));
		return list;
		
	}
	
	private ConcurrentRequestData getConcurrentRequestDataFor(Map<String, ConcurrentRequestData> apdexMap, JmeterResult result, int satisfiedThreshold, int toleratedThreshold) {
		ConcurrentRequestData apdex = apdexMap.get(result.getLabel());
		if (apdex == null) {
			apdex = new ConcurrentRequestData(satisfiedThreshold, toleratedThreshold);
			apdexMap.put(result.getLabel(), apdex);
		}
		return apdex;
	}
	
	private GroupedRequestData getGroupedRequestDataFor(Map<String, GroupedRequestData> map, JmeterResult result) {
		GroupedRequestData data = map.get(result.getLabel());
		if (data == null) {
			data = new GroupedRequestData(result.getLabel());
			map.put(result.getLabel(), data);
		}
		return data;
	}
}
