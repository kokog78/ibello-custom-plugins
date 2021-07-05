package hu.ibello.plugins.jmeter.functions;

class GeneralisedLogisticFunction2 extends GeneralisedLogisticFunction1 {

	public GeneralisedLogisticFunction2(double b, double nu, double q) {
		super(b, nu, q);
	}
	
	@Override
	public double value(double x) {
		double d = -b * x;
		d = Functions.exp(d);
		d *= q;
		d += 1.0;
		d = Functions.pow(d, Functions.reciprocal(nu));
		d = Functions.reciprocal(d);
		return d;
	}
	
}
