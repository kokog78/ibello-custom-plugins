package hu.ibello.regression.functions;

public interface Function {

	public double value(double x);
	
	public int getParameterCount();
	
	public double getParameter(int paramIndex);
	
	public void setParameter(int paramIndex, double value);
	
}
