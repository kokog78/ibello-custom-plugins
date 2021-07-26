package hu.ibello.functions;

public class LogisticErrorFunction extends Logistic5Function implements X0Function {

	@Override
	public double getX0() {
		double x0 = getC();
		double delta = x0 / 1000;
		while (value(x0) > 0.01) {
			x0 -= delta;
		}
		return x0;
	}
	
	@Override
	public Function getInverseFunction() {
		Logistic5InverseFunction inverse = new Logistic5InverseFunction();
		inverse.setParameters(getParameters());
		return inverse;
	}
}
