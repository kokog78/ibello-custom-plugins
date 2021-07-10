package hu.ibello.plugins.jmeter.model;

public class ApdexData {

	private int satisfiedCount;
	private int toleratedCount;
	private int frustratingCount;
	
	public int getSatisfiedCount() {
		return satisfiedCount;
	}

	public int getToleratedCount() {
		return toleratedCount;
	}

	public int getFrustratingCount() {
		return frustratingCount;
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
