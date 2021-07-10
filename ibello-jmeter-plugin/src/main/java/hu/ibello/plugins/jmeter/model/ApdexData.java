package hu.ibello.plugins.jmeter.model;

public class ApdexData {

	private int satisfiedCount;
	private int toleratedCount;
	private int frustratingCount;
	
	public void satisfied() {
		satisfiedCount++;
	}
	
	public void tolerated() {
		toleratedCount++;
	}
	
	public void frustrated() {
		frustratingCount++;
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
