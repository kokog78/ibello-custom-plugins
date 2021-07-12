package hu.ibello.functions;

public class CumulativeRayleighFunction implements Function, X0Function {

	protected double x0;
	protected double sigma;
	
	public double getX0() {
		return x0;
	}

	public void setX0(double x0) {
		this.x0 = x0;
	}

	public double getSigma() {
		return sigma;
	}

	public void setSigma(double sigma) {
		this.sigma = sigma;
	}

	@Override
	public double value(double x) {
		if (x <= x0) {
			return 0.0;
		}
		double delta = x - x0;
		return 1.0 - Math.exp(- delta * delta / (2 * sigma * sigma));
	}

	@Override
	public int getParameterCount() {
		return 2;
	}

	@Override
	public double getParameter(int paramIndex) {
		switch (paramIndex) {
		case 0:
			return x0;
		case 1:
			return sigma;
		}
		throw new IllegalArgumentException("Unknown parameter index: " + paramIndex);
	}

	@Override
	public void setParameter(int paramIndex, double value) {
		switch (paramIndex) {
		case 0:
			x0 = value;
			break;
		case 1:
			sigma = value;
			break;
		default:
			throw new IllegalArgumentException("Unknown parameter index: " + paramIndex);
		}
	}
	
	@Override
	public String toString() {
		String _x0 = getFormattedParameter(0);
		String _sigma = getFormattedParameter(1);
		return String.format("1 - exp(- (x - %s)^2 / (2 * %s^2))", _x0, _sigma);
	}

}
