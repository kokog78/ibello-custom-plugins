package hu.ibello.plugins.jmeter.functions;

class FiveParameterLogisticDerivate3Function implements Function {
	
	protected final double a;
	protected final double b;
	protected final double c;
	protected final double d;
	protected final double m;
	
	public FiveParameterLogisticDerivate3Function(double a, double b, double c, double d, double m) {
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
		double xb = Math.pow(x,  b);
		double divisor = Math.pow(xb + Math.pow(c,  b), m+1);
		double top = b * Math.pow(c, b * m - 1) * m * xb * (a - d);
		if (divisor == 0.0) {
			if (top < 0) {
				return Double.NEGATIVE_INFINITY;
			} else {
				return Double.POSITIVE_INFINITY;
			}
		}
		return top / divisor;
	}
	
}
