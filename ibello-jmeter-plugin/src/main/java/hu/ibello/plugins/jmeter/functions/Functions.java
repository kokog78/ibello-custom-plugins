package hu.ibello.plugins.jmeter.functions;

public class Functions {

	public static double exp(double x) {
		if (x == Double.NEGATIVE_INFINITY) {
			return 0;
		} else if (x == Double.POSITIVE_INFINITY) {
			return Double.POSITIVE_INFINITY;
		} else {
			return Math.exp(x);
		}
	}
	
	public static double ln(double x) {
		if (x < 0.0) {
			return Double.NaN;
		} else if (x == 0.0) {
			return Double.NEGATIVE_INFINITY;
		} else if (x == Double.POSITIVE_INFINITY) {
			return Double.POSITIVE_INFINITY;
		} else {
			return Math.log(x);
		}
	}
	
	public static double pow(double x, double p) {
		if (x == 0.0) {
			return 0.0;
		} else if (p == 0.0 || x == 1.0) {
			return 1.0;
		} else if (p == Double.POSITIVE_INFINITY) {
			if (x > 1.0) {
				return Double.POSITIVE_INFINITY;
			} else if (x > 0.0) {
				return 0.0;
			} else {
				return Double.NaN;
			}
		} else if (p == Double.NEGATIVE_INFINITY) {
			if (x > 1.0) {
				return 0.0;
			} else if (x > 0.0) {
				return Double.POSITIVE_INFINITY;
			} else {
				return Double.NaN;
			}
		} else {
			return Math.pow(x, p);
		}
	}
	
	public static double division(double a, double b) {
		if (b == 0.0) {
			if (a == 0.0) {
				return 0.0;
			} else if (a > 0) {
				return Double.POSITIVE_INFINITY;
			} else {
				return Double.NEGATIVE_INFINITY;
			}
		}
		return a / b;
	}
	
	public static double reciprocal(double x) {
		if (x == 0.0) {
			return Double.POSITIVE_INFINITY;
		} else if (x == Double.POSITIVE_INFINITY || x == Double.NEGATIVE_INFINITY) {
			return 0.0;
		} else {
			return 1 / x;
		}
	}
}
