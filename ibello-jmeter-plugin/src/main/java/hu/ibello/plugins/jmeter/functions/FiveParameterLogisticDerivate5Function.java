package hu.ibello.plugins.jmeter.functions;

class FiveParameterLogisticDerivate5Function implements Function {
	
	protected final double a;
	protected final double b;
	protected final double c;
	protected final double d;
	protected final double m;
	
	public FiveParameterLogisticDerivate5Function(double a, double b, double c, double d, double m) {
		super();
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.m = m;
	}
	
	@Override
	public int getParameterCount() {
		return 5;
	}

	public double getValue(double x) {
		if (c == 0.0) {
			return 0.0;
		}
		double divisor = Math.pow((1 + Math.pow(x / c, b)), m);
		if (divisor == 0.0) {
			if (a < d) {
				return Double.NEGATIVE_INFINITY;
			} else {
				return Double.POSITIVE_INFINITY;
			}
		}
		double result = a - d;
		if (result == 0.0) {
			return result;
		}
		result *= -Math.log(divisor) / divisor;
		return result;
	}
	
}
