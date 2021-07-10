package hu.ibello.plugins.jmeter.functions;

import hu.ibello.regression.functions.Function;

public class Parameter3LogisticFunction extends Parameter5LogisticFunction {

	public Parameter3LogisticFunction(double b, double c, double m) {
		super(0, 1, b, c, m);
	}
	
	@Override
	public int getParameterCount() {
		return 3;
	}
	
	@Override
	public double getParameter(int paramIndex) {
		return super.getParameter(paramIndex+2);
	}
	
	@Override
	public void setParameter(int paramIndex, double value) {
		super.setParameter(paramIndex+2, value);
	}
	
	@Override
	public Function getPartialDerivative(int paramIndex) {
		return super.getPartialDerivative(paramIndex+2);
	}

}
