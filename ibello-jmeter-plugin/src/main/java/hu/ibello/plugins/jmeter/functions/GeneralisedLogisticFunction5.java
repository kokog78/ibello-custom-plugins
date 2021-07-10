package hu.ibello.plugins.jmeter.functions;

import hu.ibello.regression.functions.Functions;

class GeneralisedLogisticFunction5 extends GeneralisedLogisticFunction0 {

	public GeneralisedLogisticFunction5(double a, double k, double b, double nu, double q) {
		super(a, k, b, nu, q);
	}
	
	@Override
	public double value(double x) {
		double base = -b * x;
		base = Functions.exp(base);
		double d = 1.0 + q * base;
		d = Functions.pow(d, 1.0 + Functions.reciprocal(nu));
		d *= nu;
		d = Functions.reciprocal(d);
		d *= a - k;
		d *= base;
		return d;
	}
	
}
