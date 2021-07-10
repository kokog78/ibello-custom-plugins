package hu.ibello.regression;

import java.util.ArrayList;
import java.util.List;

import hu.ibello.regression.DataPoint;

public class AbstractRegressionTest {

	protected List<DataPoint> points(DataPoint ... points) {
		List<DataPoint> list = new ArrayList<>();
		for (DataPoint p : points) {
			list.add(p);
		}
		return list;
	}
	
	protected DataPoint point(double x, double y) {
		return new DataPoint() {
			
			@Override
			public double getY() {
				return y;
			}
			
			@Override
			public double getX() {
				return x;
			}
		};
	}
}
