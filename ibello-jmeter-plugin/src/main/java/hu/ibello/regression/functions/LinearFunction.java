package hu.ibello.regression.functions;

public class LinearFunction implements Function {

	protected double a;
	protected double b;
	
	public double getA() {
		return a;
	}
	
	public double getB() {
		return b;
	}
	
	@Override
	public double value(double x) {
		return a + b * x;
	}

	@Override
	public int getParameterCount() {
		return 2;
	}

	@Override
	public double getParameter(int paramIndex) {
		switch (paramIndex) {
		case 0:
			return a;
		case 1:
			return b;
		}
		Functions.parameterIndexError(paramIndex);
		return 0;
	}

	@Override
	public void setParameter(int paramIndex, double value) {
		switch (paramIndex) {
		case 0:
			a = value;
			break;
		case 1:
			b = value;
			break;
		default:
			Functions.parameterIndexError(paramIndex);
		}
	}

	@Override
	public String toString() {
		return String.format("%s + %s * x", Functions.format(a), Functions.format(b));
	}
}
