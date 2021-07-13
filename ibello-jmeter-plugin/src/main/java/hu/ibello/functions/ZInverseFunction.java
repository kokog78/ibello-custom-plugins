package hu.ibello.functions;

public class ZInverseFunction extends ZFunction {

	@Override
	public double value(double x) {
		if (x <= 0) {
			return x1;
		} else if (x >= 1.0) {
			return x0;
		} else {
			return x1 - x * (x1 - x0);
		}
	}
	
	@Override
	public String toString() {
		String _x0 = getFormattedParameter(0);
		String _x1 = getFormattedParameter(1);
		return String.format("%s - x * (%s - %s)", _x1, _x1, _x0);
	}
	
}
