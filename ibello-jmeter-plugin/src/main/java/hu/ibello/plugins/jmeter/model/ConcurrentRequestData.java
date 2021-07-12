package hu.ibello.plugins.jmeter.model;

public class ConcurrentRequestData {

	private int satisfiedCount;
	private int toleratedCount;
	private int frustratingCount;
	private int errorCount;
	
	public int getSatisfiedCount() {
		return satisfiedCount;
	}

	public int getToleratedCount() {
		return toleratedCount;
	}

	public int getFrustratingCount() {
		return frustratingCount;
	}
	
	public int getErrorCount() {
		return errorCount;
	}

	public void satisfied(int count) {
		satisfiedCount += count;
	}
	
	public void tolerated(int count) {
		toleratedCount += count;
	}
	
	public void frustrated(int count) {
		frustratingCount += count;
	}
	
	public void error(int count) {
		errorCount += count;
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
