package hu.ibello.functions.impl;

import hu.ibello.functions.DataPoint;

public class DataPointImpl implements DataPoint {

	private double x;
	private double y;
	
	public DataPointImpl() {
		this(0.0, 0.0);
	}
	
	public DataPointImpl(double x, double y) {
		super();
		this.x = x;
		this.y = y;
	}
	
	public void setX(double x) {
		this.x = x;
	}

	@Override
	public double getX() {
		return x;
	}
	
	public void setY(double y) {
		this.y = y;
	}
	
	@Override
	public double getY() {
		return y;
	}
}
