package hu.ibello.plugins.jmeter.apdex;

import java.util.List;

import hu.ibello.plugins.jmeter.functions.Parameter3LogisticFunction;
import hu.ibello.plugins.jmeter.regression.Regression;
import hu.ibello.regression.DataPoint;

public class ApdexRegression extends Regression<Parameter3LogisticFunction> {

	public ApdexRegression(List<DataPoint> data) {
		super(new Parameter3LogisticFunction(30, 1, 0.01), data);
	}
	
	@Override
	public void run() {
		initialize();
		super.run();
	}
	
	protected void initialize() {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		int size = data.size();
		for (int i=0; i<size; i++) {
			double y = data.get(i).getY();
			if (y < min) {
				min = y;
			}
			if (y > max) {
				max = y;
			}
		}
		function.setY0(0.0);
		function.setY1(1.0);
		function.setB(60);
		function.setC(10);
		function.setM(0.01);
	}

}
