package hu.ibello.plugins.jmeter.functions;

import java.text.DecimalFormat;

public abstract class AbstractDifferentiableFunction implements DifferentiableFunction {

	private DecimalFormat fmt;
	
	protected void parameterIndexError(int paramIndex) {
		throw new IllegalArgumentException("Unknown parameter index: " + paramIndex);
	}
	
	protected String format(double d) {
		if (fmt == null) {
			fmt = new DecimalFormat("0.##########");
		}
		return fmt.format(d).replace(',', '.');
	}
	
}
