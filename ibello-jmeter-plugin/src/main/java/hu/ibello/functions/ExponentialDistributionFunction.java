package hu.ibello.functions;

public class ExponentialDistributionFunction implements Function, InversableFunction {

	protected double y0;
	protected double lambda;
	
	public double getY0() {
		return y0;
	}
	
	public void setY0(double y0) {
		this.y0 = y0;
	}
	
	public double getLambda() {
		return lambda;
	}
	
	public void setLambda(double lambda) {
		this.lambda = lambda;
	}
	
	@Override
	public double value(double x) {
		return y0 * (1 - Math.exp(-lambda * x));
	}

	@Override
	public int getParameterCount() {
		return 2;
	}

	@Override
	public double getParameter(int paramIndex) {
		switch (paramIndex) {
		case 0:
			return y0;
		case 1:
			return lambda;
		}
		throw new IllegalArgumentException("Unknown parameter index: " + paramIndex);
	}

	@Override
	public void setParameter(int paramIndex, double value) {
		switch (paramIndex) {
		case 0:
			y0 = value;
			break;
		case 1:
			lambda = value;
			break;
		default:
			throw new IllegalArgumentException("Unknown parameter index: " + paramIndex);
		}
	}
	
	@Override
	public String toString() {
		String _y0 = getFormattedParameter(0);
		String _lambda = getFormattedParameter(1);
		return String.format("%s * (1 - exp(- %s * x))", _y0, _lambda);
	}

	@Override
	public Function getInverseFunction() {
		ExponentialDistributionInverseFunction inverse = new ExponentialDistributionInverseFunction();
		inverse.setParameters(getParameters());
		return inverse;
	}

}
