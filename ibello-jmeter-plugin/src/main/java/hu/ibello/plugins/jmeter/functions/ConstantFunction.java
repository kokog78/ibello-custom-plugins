package hu.ibello.plugins.jmeter.functions;

import java.text.DecimalFormat;

import hu.ibello.regression.functions.Function;

public class ConstantFunction implements DifferentiableFunction {

	protected double a;
	
	public ConstantFunction(double a) {
		super();
		this.a = a;
	}

	@Override
	public double value(double x) {
		return a;
	}

	@Override
	public int getParameterCount() {
		return 1;
	}

	@Override
	public double getParameter(int paramIndex) {
		switch (paramIndex) {
		case 1:
			return a;
		default:
			throw new IllegalArgumentException("Unknown parameter index: " + paramIndex);
		}
	}

	@Override
	public void setParameter(int paramIndex, double value) {
		switch (paramIndex) {
		case 1:
			a = value;
			break;
		default:
			throw new IllegalArgumentException("Unknown parameter index: " + paramIndex);
		}
	}

	@Override
	public Function getPartialDerivative(int paramIndex) {
		switch (paramIndex) {
		case 0:
			return new ConstantFunction(0);
		case 1:
			return new ConstantFunction(1);
		default:
			throw new IllegalArgumentException("Unknown parameter index: " + paramIndex);
		}
	}
	
	@Override
	public String toString() {
		DecimalFormat fmt = new DecimalFormat("0.##########");
		return fmt.format(a);
	}

}
