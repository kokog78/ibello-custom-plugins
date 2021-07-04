package hu.ibello.plugins.jmeter.functions;

public interface DifferentiableFunction extends Function {

	public Function getPartialDerivative(int paramIndex);
	
}
