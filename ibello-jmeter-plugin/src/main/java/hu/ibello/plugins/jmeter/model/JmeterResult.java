package hu.ibello.plugins.jmeter.model;

import java.util.Date;

public class JmeterResult {

	private long timeStamp;
	private long elapsed;
	private String label;
	private boolean success;
	
	public long getTimeStamp() {
		return timeStamp;
	}
	
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public Date getStartDate() {
		return new Date(timeStamp);
	}
	
	public Date getEndDate() {
		return new Date(timeStamp + elapsed);
	}
	
	public long getElapsed() {
		return elapsed;
	}
	
	public void setElapsed(long elapsed) {
		this.elapsed = elapsed;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public boolean isSuccess() {
		return success;
	}
	
	public void setSuccess(boolean success) {
		this.success = success;
	}
	
}
