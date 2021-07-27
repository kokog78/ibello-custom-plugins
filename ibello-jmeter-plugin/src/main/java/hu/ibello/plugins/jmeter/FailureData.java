package hu.ibello.plugins.jmeter;

class FailureData {

	private double failureLimit = Double.NaN;
	private double crashLimit = Double.NaN;
	
	public double getFailureLimit() {
		return failureLimit;
	}
	
	public void updateFailureLimit(double failureLimit) {
		if (Double.isNaN(this.failureLimit) || failureLimit < this.failureLimit) {
			this.failureLimit = failureLimit;
		}
	}
	
	public double getCrashLimit() {
		return crashLimit;
	}
	
	public void updateCrashLimit(double crashLimit) {
		if (Double.isNaN(this.crashLimit) || crashLimit < this.crashLimit) {
			this.crashLimit = crashLimit;
		}
	}
	
	public boolean hasFailureLimit() {
		return !Double.isNaN(failureLimit);
	}
	
	public boolean hasCrashLimit() {
		return !Double.isNaN(crashLimit);
	}
}
