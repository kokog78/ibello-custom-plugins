package hu.ibello.functions;

public class ZFunction implements Function, X0Function {
	
	protected double x0;
	protected double x1;
	
	public double getX0() {
		return x0;
	}

	public void setX0(double x0) {
		this.x0 = x0;
	}

	public double getX1() {
		return x1;
	}

	public void setX1(double x1) {
		this.x1 = x1;
	}

	@Override
	public double value(double x) {
		if (x <= x0) {
			return 1.0;
		} else if (x >= x1) {
			return 0.0;
		} else {
			return 1 - (x - x0) / (x1 - x0);
		}
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
			return x1;
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
			x1 = value;
			break;
		default:
			throw new IllegalArgumentException("Unknown parameter index: " + paramIndex);
		}
	}
	
	@Override
	public String toString() {
		String _x0 = getFormattedParameter(0);
		String _x1 = getFormattedParameter(1);
		return String.format("1 - (x - %s) / (%s - %s)", _x0, _x1, _x0);
	}
	
	@Override
	public Function getInverseFunction() {
		ZInverseFunction inverse = new ZInverseFunction();
		inverse.setParameters(getParameters());
		return inverse;
	}
}
