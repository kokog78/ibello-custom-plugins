package hu.ibello.plugins.jmeter.functions;

public interface DifferentiableFunction extends Function {

	public int getParameterCount();
	
	public double getParameter(int paramIndex);
	
	public void setParameter(int paramIndex, double value);
	
	public Function getPartialDerivative(int paramIndex);
	
}
