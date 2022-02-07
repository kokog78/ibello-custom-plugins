package hu.ibello.plugins.jmeter.model;

import java.util.Date;

public class JmeterResult {

	private long timeStamp;
	private long elapsed;
	private String label;
	private String threadName;
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
	
	public String getThreadName() {
		return threadName;
	}
	
	public void setThreadName(String threadName) {
		this.threadName = threadName;
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
	
	public void append(JmeterResult appended) {
		if (timeStamp > appended.timeStamp) {
			timeStamp = appended.timeStamp;
		}
		elapsed += appended.elapsed;
		if (compare(label, appended.label) > 0) {
			label = appended.label;
		}
		success = success && appended.success;
		bytes += appended.bytes;
		sentBytes += appended.sentBytes;
		Connect += appended.Connect;
	}
	
	private int compare(String s1, String s2) {
		if (s1 == null) {
			if (s2 == null) {
				return 0;
			} else {
				return 1;
			}
		} else if (s2 == null) {
			return -1;
		} else {
			return s1.compareTo(s2);
		}
	}
	
}
