package hu.ibello.functions;

public class LogisticErrorFunction extends Logistic5Function implements X0Function {

	@Override
	public double getX0() {
		return getC();
	}
	
	@Override
	public Function getInverseFunction() {
		Logistic5InverseFunction inverse = new Logistic5InverseFunction();
		inverse.setParameters(getParameters());
		return inverse;
	}
}
