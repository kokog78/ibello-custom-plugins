package hu.ibello.plugins.jmeter.functions;

import hu.ibello.regression.functions.Functions;

class GeneralisedLogisticFunction3 extends GeneralisedLogisticFunction0 {

	public GeneralisedLogisticFunction3(double a, double k, double b, double nu, double q) {
		super(a, k, b, nu, q);
	}
	
	@Override
	public double value(double x) {
		double base = -b * x;
		base = Functions.exp(base);
		base *= q;
		double d = 1.0 + base;
		d = Functions.pow(d, 1.0 + Functions.reciprocal(nu));
		d *= nu;
		d = Functions.reciprocal(d);
		d *= k - a;
		d *= base * x;
		return d;
	}
	
}
