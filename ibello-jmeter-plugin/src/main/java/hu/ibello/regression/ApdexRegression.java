package hu.ibello.regression;

import java.util.List;

import hu.ibello.regression.functions.Parameter3LogisticFunction;

public class ApdexRegression extends AbstractRegression {

	private Parameter3LogisticFunction function;
	
	public ApdexRegression(List<DataPoint> data) {
		super(data);
	}
	
	@Override
	protected Type getType() {
		return Type.MARQUARDT;
	}
	
	@Override
	protected Parameter3LogisticFunction getFunction() {
		if (function == null) {
			function = new Parameter3LogisticFunction();
			function.setB(60);
			function.setC(10);
			function.setM(0.01);
		}
		return function;
	}

}
