package hu.ibello.functions;

public class ExponentialApdexInverseFunction extends ExponentialApdexFunction {

	@Override
	public double value(double y) {
		if (y >= 1.0) {
			return x0;
		}
		return - c * Math.log(y) + x0;
	}
	
	@Override
	public String toString() {
		String _x0 = getFormattedParameter(0);
		String _c = getFormattedParameter(1);
		return String.format("- %s * ln(x) + %s", _c, _x0);
	}
	
}
