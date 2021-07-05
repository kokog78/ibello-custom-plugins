package hu.ibello.plugins.jmeter.regression;

import java.text.DecimalFormat;
import java.util.List;

import hu.ibello.plugins.jmeter.functions.DifferentiableFunction;
import hu.ibello.plugins.jmeter.functions.Function;

public class Regression<F extends DifferentiableFunction> {
	
	private final static double ERROR_CHANGE_LIMIT = 0.0000001;

	protected final F function;
	protected final List<DataPoint> data;
	private double alpha;
	private double error;
	private int step = 0;
	private final DecimalFormat fmt = new DecimalFormat("0.############");
	
	public Regression(F function, List<DataPoint> data) {
		this.function = function;
		this.data = data;
	}
	
	public void run() {
		this.alpha = 0.01;
		this.error = error();
		boolean done = false;
		printParams();
		step = 0;
		while (!done) {
			done = step();
			printStep();
			printParams();
		}
	}
	
	public F getFunction() {
		return function;
	}
	
	private boolean step() {
		step++;
		int paramCount = function.getParameterCount();
		double[] derivates = derivates();
		double[] params = getParams();
		for (int j=0; j<paramCount; j++) {
			double param = function.getParameter(j+1);
			param -= derivates[j] * alpha;
			function.setParameter(j+1, param);
		}
		double e = error();
		if (error < e) {
			setParams(params);
			alpha /= 3;
			if (alpha == 0.0) {
				return true;
			}
		} else if (error - e < error * alpha * ERROR_CHANGE_LIMIT) {
			error = e;
			return true;
		} else {
			error = e;
			alpha *= 1.01;
			if (alpha > 1.0) {
				alpha = 1.0;
			}
		}
		return false;
	}
	
	private double[] derivates() {
		int size = data.size();
		int paramCount = function.getParameterCount();
		double[] result = new double[paramCount];
		for (int j=0; j<paramCount; j++) {
			result[j] = 0.0;
			Function derivate = function.getPartialDerivative(j+1);
			for (int i=0; i<size; i++) {
				DataPoint point = data.get(i);
				double y = function.value(point.getX());
				double diff = y - point.getY();
				result[j] += 2 * diff * derivate.value(point.getX()) / size;
			}
		}
		return result;
	}
	
	private double error() {
		double error = 0.0;
		int size = data.size();
		for (int i=0; i<size; i++) {
			DataPoint point = data.get(i);
			double y = function.value(point.getX());
			double diff = y - point.getY();
			error += diff*diff/size;
		}
		return error;
	}
	
	private double[] getParams() {
		int paramCount = function.getParameterCount();
		double[] result = new double[paramCount];
		for (int j=0; j<paramCount; j++) {
			result[j] = function.getParameter(j+1);
		}
		return result;
	}
	
	private void setParams(double[] params) {
		int paramCount = function.getParameterCount();
		for (int j=0; j<paramCount; j++) {
			function.setParameter(j+1, params[j]);
		}
	}
	
	private void printParams() {
		int paramCount = function.getParameterCount();
		for (int j=0; j<paramCount; j++) {
			System.out.println(String.format("\tParameter #%d = %s", j+1, fmt.format(function.getParameter(j+1))));
		}
	}
	
	private void printStep() {
		System.out.println(String.format("Step #%d", step));
		System.out.println(String.format("\tAlpha = %s", fmt.format(alpha)));
		System.out.println(String.format("\tError = %s", fmt.format(error)));
	}
}
