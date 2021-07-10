package hu.ibello.plugins.jmeter.functions;

import java.text.DecimalFormat;

import hu.ibello.regression.functions.Function;

public class LinearFunction implements DifferentiableFunction {

	protected double a;
	protected double b;
	
	public LinearFunction(double a, double b) {
		super();
		this.a = a;
		this.b = b;
	}
	
	public double getA() {
		return a;
	}
	
	public double getB() {
		return b;
	}

	@Override
	public double value(double x) {
		return a + b * x;
	}

	@Override
	public int getParameterCount() {
		return 2;
	}

	@Override
	public double getParameter(int paramIndex) {
		switch (paramIndex) {
		case 1:
			return a;
		case 2:
			return b;
		}
		throw new IllegalArgumentException("Unknown parameter index: " + paramIndex);
	}

	@Override
	public void setParameter(int paramIndex, double value) {
		switch (paramIndex) {
		case 1:
			a = value;
			break;
		case 2:
			b = value;
			break;
		default:
			throw new IllegalArgumentException("Unknown parameter index: " + paramIndex);
		}
		
	}

	@Override
	public Function getPartialDerivative(int paramIndex) {
		switch (paramIndex) {
		case 0:
			return new ConstantFunction(b);
		case 1:
			return new ConstantFunction(1);
		case 2:
			return new LinearFunction(0, 1);
		default:
			throw new IllegalArgumentException("Unknown parameter index: " + paramIndex);
		}
	}
	
	@Override
	public String toString() {
		DecimalFormat fmt = new DecimalFormat("0.##########");
		return String.format("%s + %s * x", fmt.format(a), fmt.format(b));
	}

}
