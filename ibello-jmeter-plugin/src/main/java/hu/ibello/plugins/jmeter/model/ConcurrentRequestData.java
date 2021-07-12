package hu.ibello.plugins.jmeter.model;

public class ConcurrentRequestData {

	private int satisfiedCount;
	private int toleratedCount;
	private int frustratingCount;
	private int successCount;
	private long totalElapsed;
	
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

	public void incSatisfied() {
		satisfiedCount++;
	}
	
	public void incTolerated() {
		toleratedCount++;
	}
	
	public void incFrustrated() {
		frustratingCount++;
	}
	
	public void incSuccess() {
		successCount++;
	}
	
	public void addElapsed(long elapsed) {
		totalElapsed += elapsed;
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
	
	public int count() {
		return satisfiedCount + toleratedCount + frustratingCount;
	}
}
