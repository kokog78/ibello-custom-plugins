package hu.ibello.plugins.jmeter.functions;

class GeneralisedLogisticFunction4 extends GeneralisedLogisticFunction0 {

	public GeneralisedLogisticFunction4(double a, double k, double b, double nu, double q) {
		super(a, k, b, nu, q);
	}
	
	@Override
	public double value(double x) {
		double base = -b * x;
		base = Functions.exp(base);
		base *= q;
		base += 1;
		double d = Functions.pow(base, Functions.reciprocal(nu));
		d *= nu * nu;
		d = Functions.reciprocal(d);
		d *= k - a;
		d *= Functions.ln(base);
		return d;
	}
	
}
