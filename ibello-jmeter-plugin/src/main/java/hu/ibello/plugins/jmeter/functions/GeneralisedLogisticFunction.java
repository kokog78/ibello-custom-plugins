package hu.ibello.plugins.jmeter.functions;

import hu.ibello.regression.functions.Function;
import hu.ibello.regression.functions.Functions;

public class GeneralisedLogisticFunction extends AbstractDifferentiableFunction {

	private double a;
	private double k;
	private double b;
	private double nu;
	private double q;
	
	public GeneralisedLogisticFunction(double a, double k, double b, double nu, double q) {
		super();
		this.a = a;
		this.k = k;
		this.b = b;
		this.nu = nu;
		this.q = q;
	}
	
	public void setA(double a) {
		this.a = a;
	}
	
	public void setK(double k) {
		this.k = k;
	}
	
	public void setB(double b) {
		this.b = b;
	}
	
	public void setNu(double nu) {
		this.nu = nu;
	}
	
	public void setQ(double q) {
		this.q = q;
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
		parameterIndexError(paramIndex);
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
			parameterIndexError(paramIndex);
		}
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
			return new GeneralisedLogisticFunction(1, 0, b, nu, q);
		case 2:
			// k
			return new GeneralisedLogisticFunction(0, 1, b, nu, q);
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
		parameterIndexError(paramIndex);
		return null;
	}
	
	@Override
	public String toString() {
		return String.format("%s + (%s - %s) / ((1 + %s * exp(%s * x)) ^(1/%s) )",
				format(a), format(k), format(a), format(q), format(-b), format(nu));
	}
	
}
