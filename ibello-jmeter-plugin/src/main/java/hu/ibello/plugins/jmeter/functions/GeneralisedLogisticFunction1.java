package hu.ibello.plugins.jmeter.functions;

class GeneralisedLogisticFunction1 implements Function {

	protected final double b;
	protected final double nu;
	protected final double q;
	
	public GeneralisedLogisticFunction1(double b, double nu, double q) {
		super();
		this.b = b;
		this.nu = nu;
		this.q = q;
	}
	
	@Override
	public double value(double x) {
		double d = -b * x;
		d = Functions.exp(d);
		d *= q;
		d += 1.0;
		d = Functions.pow(d, Functions.reciprocal(nu));
		d = Functions.reciprocal(d);
		d = 1 - d;
		return d;
	}
	
}
