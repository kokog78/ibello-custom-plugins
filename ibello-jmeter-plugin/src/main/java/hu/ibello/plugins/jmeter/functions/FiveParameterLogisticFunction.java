package hu.ibello.plugins.jmeter.functions;

public class FiveParameterLogisticFunction implements DifferentiableFunction {
	
	protected final double a;
	protected final double b;
	protected final double c;
	protected final double d;
	protected final double m;
	
	public FiveParameterLogisticFunction(double a, double b, double c, double d, double m) {
		super();
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.m = m;
	}

	public double getA() {
		return a;
	}

	public double getB() {
		return b;
	}

	public double getC() {
		return c;
	}

	public double getD() {
		return d;
	}

	public double getM() {
		return m;
	}
	
	@Override
	public int getParameterCount() {
		return 5;
	}

	public double getValue(double x) {
		if (c == 0.0) {
			return d;
		}
		double divisor = Math.pow((1 + Math.pow(x / c, b)), m);
		if (divisor == 0.0) {
			if (a < d) {
				return Double.NEGATIVE_INFINITY;
			} else {
				return Double.POSITIVE_INFINITY;
			}
		}
		return d + (a - d) / divisor;
	}
	
	@Override
	public Function getPartialDerivative(int paramIndex) {
		switch (paramIndex) {
		case 0:
			// a
			return new FiveParameterLogisticFunction(1, b, c, 0, m);
		case 1:
			// b
			return new FiveParameterLogisticDerivate2Function(a, b, c, d, m);
		case 2:
			// c
			return new FiveParameterLogisticDerivate3Function(a, b, c, d, m);
		case 3:
			return new FiveParameterLogisticFunction(0, b, c, 1, m);
			// d
		case 4:
			// m
			return new FiveParameterLogisticDerivate5Function(a, b, c, d, m);
		default:
			throw new IllegalArgumentException("Invalid parameter index: " + paramIndex);
		}
	}
	
}
