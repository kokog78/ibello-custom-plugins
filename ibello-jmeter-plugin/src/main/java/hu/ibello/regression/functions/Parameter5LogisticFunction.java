package hu.ibello.regression.functions;

public class Parameter5LogisticFunction implements Function {

	protected double y0;
	protected double y1;
	protected double b;
	protected double c;
	protected double m;
	
	public void setY0(double d) {
		this.y0 = d;
	}
	
	public void setY1(double a) {
		this.y1 = a;
	}
	
	public void setB(double b) {
		this.b = b;
	}
	
	public void setC(double c) {
		this.c = c;
	}
	
	public void setM(double m) {
		this.m = m;
	}
	
	@Override
	public int getParameterCount() {
		return 5;
	}
	
	@Override
	public double value(double x) {
		return y0 + (y1-y0) / Functions.pow(1 + Functions.pow(Functions.division(x, c), b), m);
	}

	@Override
	public double getParameter(int paramIndex) {
		switch (paramIndex) {
		case 0:
			return y0;
		case 1:
			return y1;
		case 2:
			return b;
		case 3:
			return c;
		case 4:
			return m;
		}
		Functions.parameterIndexError(paramIndex);
		return 0;
	}

	@Override
	public void setParameter(int paramIndex, double value) {
		switch (paramIndex) {
		case 0:
			y0 = value;
			break;
		case 1:
			y1 = value;
			break;
		case 2:
			b = value;
			break;
		case 3:
			c = value;
			break;
		case 4:
			m = value;
			break;
		default:
			Functions.parameterIndexError(paramIndex);
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s + (%s - (%s)) / ((1 + (x / (%s)) ^(%s)) ^(%s) )",
				Functions.format(y0), Functions.format(y1), Functions.format(y0), Functions.format(c), Functions.format(b), Functions.format(m));
	}

}
