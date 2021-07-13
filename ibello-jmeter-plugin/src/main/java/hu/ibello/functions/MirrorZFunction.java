package hu.ibello.functions;

public class MirrorZFunction extends ZFunction {

	@Override
	public double value(double x) {
		if (x <= x0) {
			return 0.0;
		} else if (x >= x1) {
			return 1.0;
		} else {
			return (x - x0) / (x1 - x0);
		}
	}

	@Override
	public String toString() {
		String _x0 = getFormattedParameter(0);
		String _x1 = getFormattedParameter(1);
		return String.format("(x - %s) / (%s - %s)", _x0, _x1, _x0);
	}

}
