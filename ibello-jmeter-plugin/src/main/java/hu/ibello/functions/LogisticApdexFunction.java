package hu.ibello.functions;

public class LogisticApdexFunction extends Logistic4Function implements InversableFunction {

	@Override
	public Function getInverseFunction() {
		Logistic4InverseFunction inverse = new Logistic4InverseFunction();
		inverse.setParameters(getParameters());
		return inverse;
	}

	
}
