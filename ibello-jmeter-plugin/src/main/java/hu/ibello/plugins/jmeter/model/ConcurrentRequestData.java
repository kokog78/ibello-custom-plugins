package hu.ibello.plugins.jmeter.model;

public class ConcurrentRequestData {
	
	private final int satisfiedThresholds;
	private final int toleratedThresholds;
	
	private int satisfiedCount;
	private int toleratedCount;
	private int frustratingCount;
	private int successCount;
	private long totalElapsed;
	
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
		return satisfiedCount + toleratedCount + frustratingCount;
	}
}
