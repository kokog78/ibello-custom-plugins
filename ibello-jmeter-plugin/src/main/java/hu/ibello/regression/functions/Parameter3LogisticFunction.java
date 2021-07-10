package hu.ibello.regression.functions;

public class Parameter3LogisticFunction implements Function {

	protected double b;
	protected double c;
	protected double m;
	
	public void setB(double b) {
		this.b = b;
	}
	
	public void setC(double c) {
		this.c = c;
	}
	
	public void setM(double m) {
		this.m = m;
	}
	
	@Override
	public int getParameterCount() {
		return 3;
	}
	
	@Override
	public double value(double x) {
		return 1.0 / Functions.pow(1 + Functions.pow(Functions.division(x, c), b), m);
	}

	@Override
	public double getParameter(int paramIndex) {
		switch (paramIndex) {
		case 0:
			return b;
		case 1:
			return c;
		case 2:
			return m;
		}
		Functions.parameterIndexError(paramIndex);
		return 0;
	}

	@Override
	public void setParameter(int paramIndex, double value) {
		switch (paramIndex) {
		case 0:
			b = value;
			break;
		case 1:
			c = value;
			break;
		case 2:
			m = value;
			break;
		default:
			Functions.parameterIndexError(paramIndex);
		}
	}
	
	@Override
	public String toString() {
		return String.format("1 / ((1 + (x / (%s)) ^(%s)) ^(%s) )",
				Functions.format(c), Functions.format(b), Functions.format(m));
	}
}
