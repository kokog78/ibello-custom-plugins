package hu.ibello.plugins.jmeter.functions;

import static hu.ibello.regression.functions.Functions.ln;
import static hu.ibello.regression.functions.Functions.pow;

class Parameter5LogisticFunctionB extends Parameter5LogisticFunction0 {

	public Parameter5LogisticFunctionB(double y0, double y1, double b, double c, double m) {
		super(y0, y1, b, c, m);
	}
	
	@Override
	public double value(double x) {
		double divisor = pow(pow(x, b) + pow(c, b), m + 1);
		return m * (y1-y0) * pow(c, b*m) * pow(x, b) * (ln(c) - ln(x)) / divisor;
	}

}
