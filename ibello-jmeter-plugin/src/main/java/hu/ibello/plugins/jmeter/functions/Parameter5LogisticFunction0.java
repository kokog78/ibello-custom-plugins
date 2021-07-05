package hu.ibello.plugins.jmeter.functions;

import static hu.ibello.plugins.jmeter.functions.Functions.pow;

class Parameter5LogisticFunction0 implements Function {

	protected double y0;
	protected double y1;
	protected double b;
	protected double c;
	protected double m;
	
	public Parameter5LogisticFunction0(double y0, double y1, double b, double c, double m) {
		super();
		this.y0 = y0;
		this.y1 = y1;
		this.b = b;
		this.c = c;
		this.m = m;
	}

	@Override
	public double value(double x) {
		return -b * pow(c, b*m) * m * pow(x, b-1) * (y1-y0) * pow(pow(x, b) + pow(c, b), -m-1);
	}

}
