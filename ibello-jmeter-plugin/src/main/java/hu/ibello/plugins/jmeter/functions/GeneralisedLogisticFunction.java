package hu.ibello.plugins.jmeter.functions;

public class GeneralisedLogisticFunction implements DifferentiableFunction {

	private final double a;
	private final double k;
	private final double b;
	private final double nu;
	private final double q;
	
	public GeneralisedLogisticFunction(double a, double k, double b, double nu, double q) {
		super();
		this.a = a;
		this.k = k;
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
		d *= k - a;
		d += a;
		return d;
	}
	
	@Override
	public Function getPartialDerivative(int paramIndex) {
		switch (paramIndex) {
		case 0:
			// x
			return new GeneralisedLogisticFunction0(a, k, b, nu, q);
		case 1:
			// a
			return new GeneralisedLogisticFunction1(b, nu, q);
		case 2:
			// k
			return new GeneralisedLogisticFunction2(b, nu, q);
		case 3:
			// b
			return new GeneralisedLogisticFunction3(a, k, b, nu, q);
		case 4:
			// nu
			return new GeneralisedLogisticFunction4(a, k, b, nu, q);
		case 5:
			// q
			return new GeneralisedLogisticFunction5(a, k, b, nu, q);
		}
		return null;
	}
	
}
