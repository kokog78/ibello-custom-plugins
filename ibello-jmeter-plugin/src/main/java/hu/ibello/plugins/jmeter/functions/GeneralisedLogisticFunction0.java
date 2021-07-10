package hu.ibello.plugins.jmeter.functions;

import hu.ibello.regression.functions.Function;
import hu.ibello.regression.functions.Functions;

class GeneralisedLogisticFunction0 implements Function {

	protected double a;
	protected double k;
	protected double b;
	protected double nu;
	protected double q;
	
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

	@Override
	public int getParameterCount() {
		return 5;
	}

	@Override
	public double getParameter(int paramIndex) {
		switch (paramIndex) {
		case 1:
			return a;
		case 2:
			return k;
		case 3:
			return b;
		case 4:
			return nu;
		case 5:
			return q;
		}
		Functions.parameterIndexError(paramIndex);
		return 0;
	}
	
	@Override
	public void setParameter(int paramIndex, double value) {
		switch (paramIndex) {
		case 1:
			a = value;
			break;
		case 2:
			k = value;
			break;
		case 3:
			b = value;
			break;
		case 4:
			nu = value;
			break;
		case 5:
			q = value;
			break;
		default:
			Functions.parameterIndexError(paramIndex);
		}
	}
	
}
