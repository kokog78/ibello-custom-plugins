package hu.ibello.plugins.jmeter.functions;

import hu.ibello.regression.functions.Function;

public interface DifferentiableFunction extends Function {
	
	public Function getPartialDerivative(int paramIndex);
	
}
