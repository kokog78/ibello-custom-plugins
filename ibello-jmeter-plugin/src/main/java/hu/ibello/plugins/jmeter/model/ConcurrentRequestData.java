package hu.ibello.plugins.jmeter.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ConcurrentRequestData {
	
	private final int satisfiedThresholds;
	private final int toleratedThresholds;
	
	private int satisfiedCount;
	private int toleratedCount;
	private int frustratingCount;
	private int successfulSatisfiedCount;
	private int successfulToleratedCount;
	private int successfulFrustratingCount;
	private int successCount;
	private long totalElapsed;
	private long minElapsed = -1;
	private long maxElapsed = 0;
	private long startTimeMs = -1;
	private long endTimeMs = 0;
	private long receivedBytes = 0;
	private long sentBytes = 0;
	private List<Long> elapsedList = new ArrayList<>();
	
	public ConcurrentRequestData(int satisfiedThresholds, int toleratedThresholds) {
		super();
		this.satisfiedThresholds = satisfiedThresholds;
		this.toleratedThresholds = toleratedThresholds;
	}
	
	public void register(JmeterResult result) {
		if (!result.isSuccess()) {
			frustratingCount++;
		} else if (result.getElapsed() <= satisfiedThresholds) {
			satisfiedCount++;
		} else if (result.getElapsed() <= toleratedThresholds) {
			toleratedCount++;
		} else {
			frustratingCount++;
		}
		if (result.isSuccess()) {
			successCount++;
			if (result.getElapsed() <= satisfiedThresholds) {
				successfulSatisfiedCount++;
			} else if (result.getElapsed() <= toleratedThresholds) {
				successfulToleratedCount++;
			} else {
				successfulFrustratingCount++;
			}
		}
		totalElapsed += result.getElapsed();
		elapsedList.add(result.getElapsed());
		if (minElapsed < 0 || result.getElapsed() < minElapsed) {
			minElapsed = result.getElapsed();
		}
		if (result.getElapsed() > maxElapsed) {
			maxElapsed = result.getElapsed();
		}
		if (startTimeMs < 0 || startTimeMs > result.getTimeStamp()) {
			startTimeMs = result.getTimeStamp();
		}
		long endTime = result.getEndTime();
		if (endTimeMs < endTime) {
			endTimeMs = endTime;
		}
		receivedBytes += result.getBytes();
		sentBytes += result.getSentBytes();
	}

	public int getSatisfiedCount() {
		return satisfiedCount;
	}

	public int getToleratedCount() {
		return toleratedCount;
	}

	public int getFrustratingCount() {
		return frustratingCount;
	}
	
	public int getSuccessCount() {
		return successCount;
	}
	
	public int getFailureCount() {
		return count() - successCount;
	}
	
	public long getMinElapsed() {
		return minElapsed < 0 ? 0 : minElapsed;
	}
	
	public long getMaxElapsed() {
		return maxElapsed;
	}
	
	public double getAverageElapsed() {
		double result = totalElapsed;
		result /= count();
		return result;
	}
	
	public double get90PercentElapsed() {
		int count = count();
		if (count == 0) {
			return 0;
		}
		Collections.sort(elapsedList);
		double or = 0.9 * (count+1);
		int loRank = (int)Math.floor(or);
		int hiRank = (int)Math.ceil(or);
		if (hiRank > count) {
			hiRank = count;
		}
		long loScore = elapsedList.get(loRank-1);
		long hiScore = elapsedList.get(hiRank-1);
		long diff = hiScore - loScore;
		double mod = or - loRank;
		return loScore + mod * diff;
	}
	
	public long getStartTimeMs() {
		return startTimeMs;
	}
	
	public long getEndTimeMs() {
		return endTimeMs;
	}
	
	public Date getStartDate() {
		return new Date(startTimeMs);
	}
	
	public Date getEndDate() {
		return new Date(endTimeMs);
	}
	
	public long getDurationMs() {
		return endTimeMs - startTimeMs;
	}
	
	public long getDurationS() {
		return Math.round(getDurationMs() / 1000.0);
	}
	
	public boolean hasFailure() {
		return successCount < count();
	}
	
	public double getThroughput() {
		double result = count();
		result /= getDurationMs();
		result *= 1000.0;
		return result;
	}
	
	public double getApdex() {
		return getApdex(satisfiedCount, toleratedCount, frustratingCount);
	}
	
	public double getSuccessfulApdex() {
		return getApdex(successfulSatisfiedCount, successfulToleratedCount, successfulFrustratingCount);
	}
	
	public double getFailureRatio() {
		double failures = getFailureCount();
		failures /= count();
		return failures;
	}
	
	public int count() {
		return elapsedList.size();
	}
	
	public double getNetworkSent() {
		double result = sentBytes;
		result /= getDurationMs();
		result *= 1000.0 / 1024.0;
		return result;
	}

	public double getNetworkReceived() {
		double result = receivedBytes;
		result /= getDurationMs();
		result *= 1000.0 / 1024.0;
		return result;
	}

	private double getApdex(int satisfiedCount, int toleratedCount, int frustratingCount) {
		double result = satisfiedCount + 0.5 * toleratedCount;
		if (result >= 0.0) {
			result /= (satisfiedCount + toleratedCount + frustratingCount);
		}
		return result;
	}
	
}
