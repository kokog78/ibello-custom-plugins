package hu.ibello.plugins.jmeter.functions;

import static hu.ibello.plugins.jmeter.functions.Functions.ln;
import static hu.ibello.plugins.jmeter.functions.Functions.pow;

class Parameter5LogisticFunctionM extends Parameter5LogisticFunction0 {

	public Parameter5LogisticFunctionM(double y0, double y1, double b, double c, double m) {
		super(y0, y1, b, c, m);
	}
	
	@Override
	public double value(double x) {
		double seed = pow(x, b) + pow(c, b);
		double divisor = pow(seed, m);
		return pow(c, b*m) * (y1-y0) * (b * ln(c) - ln(seed)) / divisor;
	}

}
