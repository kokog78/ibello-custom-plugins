package hu.ibello.plugins.jmeter.functions;

import static hu.ibello.regression.functions.Functions.pow;

import hu.ibello.regression.functions.Function;
import hu.ibello.regression.functions.Functions;

class Parameter5LogisticFunction0 implements Function {

	protected double y0;
	protected double y1;
	protected double b;
	protected double c;
	protected double m;
	
	public Parameter5LogisticFunction0(double y0, double y1, double b, double c, double m) {
		super();
		this.y0 = y0;
		this.y1 = y1;
		this.b = b;
		this.c = c;
		this.m = m;
	}

	@Override
	public double value(double x) {
		return -b * pow(c, b*m) * m * pow(x, b-1) * (y1-y0) * pow(pow(x, b) + pow(c, b), -m-1);
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
		Functions.parameterIndexError(paramIndex);
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
			Functions.parameterIndexError(paramIndex);
		}
	}

}
