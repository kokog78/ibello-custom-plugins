package hu.ibello.functions;

public class ExponentialApdexFunction implements Function, InversableFunction {

	protected double x0;
	protected double c;
	
	public double getX0() {
		return x0;
	}

	public void setX0(double x0) {
		this.x0 = x0;
	}

	public double getC() {
		return c;
	}

	public void setC(double c) {
		this.c = c;
	}

	@Override
	public double value(double x) {
		if (x <= x0) {
			return 1.0;
		}
		return Math.exp(- (x - x0) / c);
	}

	@Override
	public int getParameterCount() {
		return 2;
	}

	@Override
	public double getParameter(int paramIndex) {
		switch (paramIndex) {
		case 0:
			return x0;
		case 1:
			return c;
		}
		throw new IllegalArgumentException("Unknown parameter index: " + paramIndex);
	}

	@Override
	public void setParameter(int paramIndex, double value) {
		switch (paramIndex) {
		case 0:
			x0 = value;
			break;
		case 1:
			c = value;
			break;
		default:
			throw new IllegalArgumentException("Unknown parameter index: " + paramIndex);
		}
	}
	
	@Override
	public String toString() {
		String _x0 = getFormattedParameter(0);
		String _c = getFormattedParameter(1);
		return String.format("exp(- (x - %s) / %s)", _x0, _c);
	}
	
	@Override
	public Function getInverseFunction() {
		ExponentialApdexInverseFunction inverse = new ExponentialApdexInverseFunction();
		inverse.setParameters(getParameters());
		return inverse;
	}

}
