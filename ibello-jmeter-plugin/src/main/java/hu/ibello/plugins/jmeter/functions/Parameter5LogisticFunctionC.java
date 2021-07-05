package hu.ibello.plugins.jmeter.functions;

import static hu.ibello.plugins.jmeter.functions.Functions.pow;

class Parameter5LogisticFunctionC extends Parameter5LogisticFunction0 {

	public Parameter5LogisticFunctionC(double y0, double y1, double b, double c, double m) {
		super(y0, y1, b, c, m);
	}
	
	@Override
	public double value(double x) {
		double divisor = pow(pow(x, b) + pow(c, b), m + 1);
		return b * pow(c, b*m - 1) * m * pow(x, b) * (y1-y0) / divisor;
	}

}
