package hu.ibello.functions;

@Deprecated
public class LogisticApdexInverseFunction extends LogisticApdexFunction {

	@Override
	public double value(double x) {
		return c * Math.pow((1 / Math.pow(x, 1/m)) - 1, 1/b);
	}

	@Override
	public String toString() {
		String _b = getFormattedParameter(0);
		String _c = getFormattedParameter(1);
		String _m = getFormattedParameter(2);
		return String.format("%s * ((1 / (x ^ (1/%s))) - 1) ^(1/%s)", _c, _m, _b);
	}
	
}
