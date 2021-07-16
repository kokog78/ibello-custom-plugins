package hu.ibello.functions;

public class DataPointImpl implements DataPoint {

	private final double x;
	private final double y;
	
	public DataPointImpl(double x, double y) {
		super();
		this.x = x;
		this.y = y;
	}

	@Override
	public double getX() {
		return x;
	}
	@Override
	public double getY() {
		return y;
	}
}
