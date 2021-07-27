package hu.ibello.plugins.jmeter;

class FailureData {

	private double failureLimit = Double.NaN;
	private double crashLimit = Double.NaN;
	
	public double getFailureLimit() {
		return failureLimit;
	}
	
	public void setFailureLimit(double failureLimit) {
		this.failureLimit = failureLimit;
	}
	
	public double getCrashLimit() {
		return crashLimit;
	}
	
	public void setCrashLimit(double crashLimit) {
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
