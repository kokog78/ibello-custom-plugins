package hu.ibello.plugins.jmeter.model;

import java.util.Date;

public class GroupedRequestData {

	private final String label;
	private Date startDate;
	private Date endDate;
	private Date firstSuccessDate;
	private int count;
	private int failedCount;
	
	public GroupedRequestData(String label) {
		super();
		this.label = label;
	}

	public void register(JmeterResult result) {
		Date sd = result.getStartDate();
		if (startDate == null || sd.before(startDate)) {
			startDate = sd;
		}
		Date ed = result.getEndDate();
		if (endDate == null || ed.after(endDate)) {
			endDate = ed;
		}
		count++;
		if (result.isSuccess()) {
			if (firstSuccessDate == null || sd.before(firstSuccessDate)) {
				firstSuccessDate = sd;
			}
		} else {
			failedCount++;
		}
	}
	
	public String getLabel() {
		return label;
	}
	
	public Date getStartDate() {
		return startDate;
	}
	
	public Date getEndDate() {
		return endDate;
	}
	
	public Date getFirstSuccessDate() {
		return firstSuccessDate;
	}
	
	public int getCount() {
		return count;
	}
	
	public int getFailedCount() {
		return failedCount;
	}
}
