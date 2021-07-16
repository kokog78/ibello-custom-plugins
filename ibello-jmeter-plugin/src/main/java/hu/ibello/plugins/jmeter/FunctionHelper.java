package hu.ibello.plugins.jmeter;

import java.util.ArrayList;
import java.util.List;

import hu.ibello.functions.CumulativeRayleighFunction;
import hu.ibello.functions.DataPoint;
import hu.ibello.functions.ExponentialApdexFunction;
import hu.ibello.functions.Function;
import hu.ibello.functions.Logistic4Function;
import hu.ibello.functions.MirrorZFunction;
import hu.ibello.functions.ZFunction;

public class FunctionHelper {

	public ExponentialApdexFunction getExponentialApdexFunction(List<DataPoint> points) {
		ExponentialApdexFunction function = new ExponentialApdexFunction();
		double x0 = Double.NaN;
		double yLimit = 1 / Math.E;
		double x1 = Double.NaN;
		double y1 = Double.NaN;
		double cPluszX0 = Double.NaN;
		for (DataPoint point : points) {
			if (!Double.isNaN(point.getX()) && !Double.isNaN(point.getY())) {
				if (point.getY() == 1.0) {
					x0 = point.getX();
				}
				if (point.getY() > yLimit) {
					x1 = point.getX();
					y1 = point.getY();
				} else if (Double.isNaN(cPluszX0) && !Double.isNaN(x1) && !Double.isNaN(y1)) {
					cPluszX0 = (y1 - yLimit) * (point.getX() - x1) / (y1 - point.getY()) + x1;
				}
			}
		}
		if (Double.isNaN(x0)) {
			x0 = 0.0;
		}
		if (Double.isNaN(cPluszX0)) {
			cPluszX0 = x0 + 1;
		}
		function.setX0(x0);
		function.setC(cPluszX0 - x0);
		return function;
	}
	
	public Function getLogisticApdexFunction(List<DataPoint> points) {
		Logistic4Function function = new Logistic4Function();
		double y1 = 0.0;
		for (DataPoint point : points) {
			y1 = Math.max(y1, point.getY());
		}
		double b = 2;
		double sumC = 0.0;
		int countC = 0;
		for (DataPoint point : points) {
			if (point.getY() < y1) {
				double c = Math.pow(y1 / point.getY(), 1/b) - 1.0;
				if (c > 0.0) {
					c = point.getX() / c;
					sumC += c;
					countC++;
				}
			}
		}
		double c;
		if (countC > 0) {
			c = sumC / countC;
		} else {
			c = 20;
		}
		function.setY0(0);
		function.setY1(y1);
		function.setB(b);
		function.setC(c);
		return function;
	}
	
	public CumulativeRayleighFunction getCumulativeRayleighFunction(List<DataPoint> points) {
		CumulativeRayleighFunction function = new CumulativeRayleighFunction();
		double x0 = 0;
		DataPoint lastPoint = null;
		double sigma = Double.NaN;
		for (DataPoint point : points) {
			if (point.getY() == 0.0) {
				x0 = point.getX();
			} else if (lastPoint == null) {
				lastPoint = point;
			} else if (point.getY() < 1.0) {
				lastPoint = point;
			}
		}
		if (lastPoint != null) {
			double delta = 1 - lastPoint.getY();
			if (delta == 0.0) {
				delta = 0.01;
			}
			sigma = (lastPoint.getX() - x0) / Math.sqrt(- 2 * Math.log(delta));
		} else {
			sigma = 50;
		}
		function.setX0(x0);
		function.setSigma(sigma);
		return function;
	}
	
	public MirrorZFunction getMirrorZFunction(List<DataPoint> points) {
		MirrorZFunction function = new MirrorZFunction();
		double x0 = 0;
		double x1 = Double.NaN;
		DataPoint lastPoint = null;
		for (DataPoint point : points) {
			if (point.getY() == 0.0) {
				x0 = point.getX();
			} else if (lastPoint == null) {
				lastPoint = point;
			} else if (point.getY() < 1.0) {
				lastPoint = point;
			}
		}
		if (lastPoint != null) {
			x1 = x0 + (lastPoint.getX() - x0) / lastPoint.getY();
		} else {
			x1 = 1.0;
		}
		function.setX0(x0);
		function.setX1(x1);
		return function;
	}
	
	public ZFunction getZFunction(List<DataPoint> points) {
		ZFunction function = new ZFunction();
		double x0 = 0;
		double x1 = Double.NaN;
		DataPoint lastPoint = null;
		for (DataPoint point : points) {
			if (point.getY() >= 1.0) {
				x0 = point.getX();
			} else if (lastPoint == null) {
				lastPoint = point;
			} else if (point.getY() > 0.0) {
				lastPoint = point;
			}
		}
		if (lastPoint != null) {
			x1 = x0 + (lastPoint.getX() - x0) / (1.0 - lastPoint.getY());
		} else {
			x1 = 1.0;
		}
		function.setX0(x0);
		function.setX1(x1);
		return function;
	}
	
	public double calculareR2(Function function, List<DataPoint> points) {
		double r2 = 0.0;
		double ymean = 0.0;
		int count = 0;
		List<DataPoint> validPoints = new ArrayList<>();
		for (DataPoint point : points) {
			double y = function.value(point.getX());
			if (!Double.isNaN(y) && !Double.isNaN(point.getY())) {
				r2 += Math.pow(y - point.getY(), 2);
				ymean += point.getY();
				count++;
				validPoints.add(point);
			}
		}
		ymean /= count;
		double divisor = 0.0;
		for (DataPoint point : validPoints) {
			divisor += Math.pow(ymean - point.getY(), 2);
		}
		r2 = 1.0 - (r2 / divisor);
		return r2;
	}
}
