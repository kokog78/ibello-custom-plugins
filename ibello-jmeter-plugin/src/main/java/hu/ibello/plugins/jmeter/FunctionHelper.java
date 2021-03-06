package hu.ibello.plugins.jmeter;

import java.util.ArrayList;
import java.util.List;

import hu.ibello.functions.CumulativeRayleighFunction;
import hu.ibello.functions.DataPoint;
import hu.ibello.functions.ExponentialApdexFunction;
import hu.ibello.functions.ExponentialDistributionFunction;
import hu.ibello.functions.ExponentialDistributionInverseFunction;
import hu.ibello.functions.Function;
import hu.ibello.functions.Logistic4Function;
import hu.ibello.functions.LogisticApdexFunction;
import hu.ibello.functions.LogisticErrorFunction;
import hu.ibello.functions.MirrorZFunction;
import hu.ibello.functions.PowerFunction;
import hu.ibello.functions.RegressionTool;
import hu.ibello.functions.X0Function;
import hu.ibello.functions.ZFunction;
import hu.ibello.functions.impl.DataPointImpl;

public class FunctionHelper {
	
	private final RegressionTool regression;
	
	public FunctionHelper(RegressionTool regression) {
		super();
		this.regression = regression;
	}

	public ExponentialApdexFunction getExponentialApdexFunction(List<? extends DataPoint> points) {
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
	
	public LogisticApdexFunction getLogisticApdexFunction(List<? extends DataPoint> points) {
		LogisticApdexFunction function = new LogisticApdexFunction();
		double y0 = 1.0;
		double y1 = 0.0;
		for (DataPoint point : points) {
			y0 = Math.min(y0, point.getY());
			y1 = Math.max(y1, point.getY());
		}
		if (y0 < 0.01) {
			y0 = 0;
		}
		double b = Double.NaN;
		// finding c; if x = c then y = y1/2
		double c = findLogisticApdexFunctionMean(points, y0, y1);
		if (Double.isNaN(c)) {
			// calculating (x / c) ^ b
			List<DataPoint> internalPoints = new ArrayList<>();
			boolean hasDifferentValues = false;
			Double yPrev = null;
			for (DataPoint point : points) {
				double y = (y1 / (point.getY()-y0)) - 1;
				if (Double.isFinite(y)) {
					DataPoint p = new DataPointImpl(point.getX(), y);
					internalPoints.add(p);
					hasDifferentValues |= (yPrev != null) && (yPrev.doubleValue() != y);
					yPrev = y;
				}
			}
			if (hasDifferentValues) {
				// fitting power function - only if we have at least 2 different values
				PowerFunction power = new PowerFunction(1, 2);
				regression.getNonLinearRegression(power, internalPoints).run();
				b = power.getB();
				c = 1 / Math.pow(power.getA(), 1/b);
			}
			if (Double.isNaN(b)) {
				b = 2;
			}
			if (Double.isNaN(c)) {
				// calculating average of c
				c = calculateAverage(points, 20, logisticApdexCCalculator(y0, y1, b));
			}
		} else {
			b = calculateAverage(points, Double.NaN, logisticApdexBCalculator(y0, y1, c));
			if (Double.isNaN(b)) {
				b = 2;
			}
		}
		function.setY0(y0);
		function.setY1(y1);
		function.setB(b);
		function.setC(c);
		return function;
	}
	
	private double findLogisticApdexFunctionMean(List<? extends DataPoint> points, double ymin, double ymax) {
		double yhalf = ymin + (ymax - ymin) / 2;
		double x1 = 0;
		double y1 = 0;
		for (DataPoint point : points) {
			if (point.getY() > yhalf) {
				x1 = point.getX();
				y1 = point.getY();
			} else if (point.getY() == yhalf) {
				return point.getX();
			} else {
				return x1 + (point.getX() - x1) * (y1 - yhalf) / (y1 - point.getY());
			}
		}
		return Double.NaN;
	}
	
	private java.util.function.Function<DataPoint, Double> logisticApdexBCalculator(double y0, double y1, double c) {
		return point -> Math.log(((y1-y0) / (point.getY()-y0)) - 1) / Math.log(point.getX() / c);
	}
	
	private java.util.function.Function<DataPoint, Double> logisticApdexCCalculator(double y0, double y1, double b) {
		return point -> {
			double y = point.getY() - y0;
			double c1 = Math.pow(((y1-y0) / y)-1, 1/b);
			if (c1 > 0.0) {
				return point.getX() / c1;
			} else {
				return Double.NaN;
			}
		};
	}
	
	public X0Function getLogisticErrorFunction(List<? extends DataPoint> points) {
		LogisticErrorFunction function = new LogisticErrorFunction();
		double c = Double.NaN;
		double c2 = 1.0;
		double m = 0.003;
		for (DataPoint point : points) {
			if (point.getY() == 0.0) {
				c = point.getX();
			} else if (point.getY() < 0.1) {
				c2 = point.getX();
			}
		}
		if (Double.isNaN(c)) {
			c = c2;
		}
		double sumB = 0.0;
		int countB = 0;
		for (DataPoint point : points) {
			double ratio = 1 / (1 - point.getY());
			double b = Math.log(Math.pow(ratio, 1/m) - 1) / Math.log(point.getX() / c);
			if (!Double.isNaN(b) && !Double.isInfinite(b)) {
				sumB += b;
				countB++;
			}
		}
		double b;
		if (countB > 0) {
			b = sumB / countB;
		} else {
			b = 200;
		}
		function.setY0(1);
		function.setY1(0);
		function.setB(b);
		function.setC(c);
		function.setM(m);
		return function;
	}
	
	public Function getLogisticThroughputFunction(List<? extends DataPoint> points) {
		Logistic4Function function = new Logistic4Function();
		double y0 = 0;
		for (DataPoint point : points) {
			if (point.getY() > y0) {
				y0 = point.getY();
			}
		}
		double b;
		double c = findLogisticThroughputFunctionMean(points, y0);
		if (Double.isNaN(c)) {
			b = 1.0;
			c = calculateAverage(points, 10.0, logisticThroughputCCalculator(y0, b));
		} else {
			b = calculateAverage(points, 1.0, logisticThroughputBCalculator(y0, c));
		}
		function.setY0(y0);
		function.setY1(0.0);
		function.setB(b);
		function.setC(c);
		return function;
	}
	
	private double findLogisticThroughputFunctionMean(List<? extends DataPoint> points, double ymax) {
		double yhalf = ymax / 2;
		double x1 = 0;
		double y1 = 0;
		for (DataPoint point : points) {
			if (point.getY() < yhalf) {
				x1 = point.getX();
				y1 = point.getY();
			} else if (point.getY() == yhalf) {
				return point.getX();
			} else {
				return x1 + (point.getX() - x1) * (yhalf - y1) / (point.getY() - y1);
			}
		}
		return Double.NaN;
	}
	
	private java.util.function.Function<DataPoint, Double> logisticThroughputBCalculator(double y0, double c) {
		return point -> {
			double r = point.getY() / y0;
			return Math.log(r / (1-r)) / Math.log(point.getX() / c);
		};
	}
	
	private java.util.function.Function<DataPoint, Double> logisticThroughputCCalculator(double y0, double b) {
		return point -> point.getX() / Math.pow((y0 / (y0 - point.getY())) - 1, 1 / b);
	}
	
	public CumulativeRayleighFunction getCumulativeRayleighFunction(List<? extends DataPoint> points) {
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
	
	public MirrorZFunction getMirrorZFunction(List<? extends DataPoint> points) {
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
	
	public ZFunction getZFunction(List<? extends DataPoint> points) {
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
	
	public ExponentialDistributionFunction getExponentialDistributionFunction(List<? extends DataPoint> points) {
		ExponentialDistributionFunction function = new ExponentialDistributionFunction();
		double y0 = 0;
		for (DataPoint point : points) {
			if (y0 < point.getY()) {
				y0 = point.getY();
			}
		}
		if (y0 <= 0.0) {
			y0 = 1.0;
		}
		ExponentialDistributionInverseFunction inverse = new ExponentialDistributionInverseFunction();
		inverse.setY0(y0);
		double lambda = calculateAverage(points, 1.0, point -> {
			inverse.setLambda(point.getX());
			return inverse.value(point.getY());
		});
		function.setY0(y0);
		function.setLambda(lambda);
		return function;
	}
	
	public double calculateR2(Function function, List<? extends DataPoint> points) {
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
	
	private double calculateAverage(List<? extends DataPoint> points, double defaultValue, java.util.function.Function<DataPoint, Double> calculator) {
		double sum = 0;
		int count = 0;
		for (DataPoint point : points) {
			double value = calculator.apply(point);
			if (!Double.isNaN(value) && !Double.isInfinite(value)) {
				sum += value;
				count++;
			}
		}
		if (count > 0) {
			return sum / count;
		} else {
			return defaultValue;
		}
	}
}
