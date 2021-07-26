package hu.ibello.plugins.jmeter.model;

import java.util.Date;

public class JmeterResult {

	private long timeStamp;
	private long elapsed;
	private String label;
	private boolean success;
	private long bytes;
	private long sentBytes;
	private long Connect;
	
	public long getTimeStamp() {
		return timeStamp;
	}
	
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public long getEndTime() {
		return timeStamp + elapsed;
	}
	
	public Date getStartDate() {
		return new Date(timeStamp);
	}
	
	public Date getEndDate() {
		return new Date(getEndTime());
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
	
	public long getBytes() {
		return bytes;
	}
	
	public void setBytes(long bytes) {
		this.bytes = bytes;
	}
	
	public long getSentBytes() {
		return sentBytes;
	}
	
	public void setSentBytes(long sentBytes) {
		this.sentBytes = sentBytes;
	}
	
	public long getConnect() {
		return Connect;
	}
	
	public void setConnect(long connect) {
		Connect = connect;
	}
	
}
