package hu.ibello.plugins.jmeter.functions;

public class Parameter5LogisticFunction extends AbstractDifferentiableFunction {

	protected double y0;
	protected double y1;
	protected double b;
	protected double c;
	protected double m;
	
	public Parameter5LogisticFunction(double y0, double y1, double b, double c, double m) {
		super();
		this.y0 = y0;
		this.y1 = y1;
		this.b = b;
		this.c = c;
		this.m = m;
	}
	
	public void setY0(double d) {
		this.y0 = d;
	}
	
	public void setY1(double a) {
		this.y1 = a;
	}
	
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
	public double value(double x) {
		return y0 + (y1-y0) / Functions.pow(1 + Functions.pow(Functions.division(x, c), b), m);
	}

	@Override
	public int getParameterCount() {
		return 5;
	}

	@Override
	public double getParameter(int paramIndex) {
		switch (paramIndex) {
		case 1:
			return y0;
		case 2:
			return y1;
		case 3:
			return b;
		case 4:
			return c;
		case 5:
			return m;
		}
		parameterIndexError(paramIndex);
		return 0;
	}

	@Override
	public void setParameter(int paramIndex, double value) {
		switch (paramIndex) {
		case 1:
			y0 = value;
			break;
		case 2:
			y1 = value;
			break;
		case 3:
			b = value;
			break;
		case 4:
			c = value;
			break;
		case 5:
			m = value;
			break;
		default:
			parameterIndexError(paramIndex);
		}
	}

	@Override
	public Function getPartialDerivative(int paramIndex) {
		switch (paramIndex) {
		case 0:
			return new Parameter5LogisticFunction0(y0, y1, b, c, m);
		case 1:
			return new Parameter5LogisticFunction(1, 0, b, c, m);
		case 2:
			return new Parameter5LogisticFunction(0, 1, b, c, m);
		case 3:
			return new Parameter5LogisticFunctionB(y0, y1, b, c, m);
		case 4:
			return new Parameter5LogisticFunctionC(y0, y1, b, c, m);
		case 5:
			return new Parameter5LogisticFunctionM(y0, y1, b, c, m);
		}
		return null;
	}
	
	@Override
	public String toString() {
		return String.format("%s + (%s - (%s)) / ((1 + (x / (%s)) ^(%s)) ^(%s) )",
				format(y0), format(y1), format(y0), format(c), format(b), format(m));
	}

}
