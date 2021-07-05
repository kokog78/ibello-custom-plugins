package hu.ibello.plugins.jmeter.functions;

class GeneralisedLogisticFunction0 implements Function {

	protected final double a;
	protected final double k;
	protected final double b;
	protected final double nu;
	protected final double q;
	
	public GeneralisedLogisticFunction0(double a, double k, double b, double nu, double q) {
		super();
		this.a = a;
		this.k = k;
		this.b = b;
		this.nu = nu;
		this.q = q;
	}
	
	@Override
	public double value(double x) {
		double base = -b * x;
		base = Functions.exp(base);
		double d = 1.0 + q * base;
		d = Functions.pow(d, 1.0 + Functions.reciprocal(nu));
		d *= nu;
		d = Functions.reciprocal(d);
		d *= k - a;
		d *= base * q * b;
		return d;
	}
	
}
