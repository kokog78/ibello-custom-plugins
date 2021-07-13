package hu.ibello.plugins.jmeter.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConcurrentRequestData {
	
	private final int satisfiedThresholds;
	private final int toleratedThresholds;
	
	private int satisfiedCount;
	private int toleratedCount;
	private int frustratingCount;
	private int successCount;
	private long totalElapsed;
	private List<Long> elapsedList = new ArrayList<>();
	
	public ConcurrentRequestData(int satisfiedThresholds, int toleratedThresholds) {
		super();
		this.satisfiedThresholds = satisfiedThresholds;
		this.toleratedThresholds = toleratedThresholds;
	}
	
	public void register(JmeterResult result) {
		if (result.getElapsed() <= satisfiedThresholds) {
			satisfiedCount++;
		} else if (result.getElapsed() <= toleratedThresholds) {
			toleratedCount++;
		} else {
			frustratingCount++;
		}
		if (result.isSuccess()) {
			successCount++;
		}
		totalElapsed += result.getElapsed();
		elapsedList.add(result.getElapsed());
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

	public boolean hasFailure() {
		return successCount < count();
	}
	
	public double getApdex() {
		double result = satisfiedCount + 0.5 * toleratedCount;
		if (result >= 0.0) {
			result /= count();
		}
		return result;
	}
	
	public double getFailureRatio() {
		double failures = getFailureCount();
		failures /= count();
		return failures;
	}
	
	public int count() {
		return elapsedList.size();
	}
}
