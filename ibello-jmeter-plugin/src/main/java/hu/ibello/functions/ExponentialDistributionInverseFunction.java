package hu.ibello.functions;

public class ExponentialDistributionInverseFunction extends ExponentialDistributionFunction {

	@Override
	public double value(double x) {
		return - Math.log(1 - (x / y0)) / lambda;
	}

	@Override
	public String toString() {
		String _y0 = getFormattedParameter(0);
		String _lambda = getFormattedParameter(1);
		return String.format("- ln(1 - x / %x) / %s", _y0, _lambda);
	}

}
