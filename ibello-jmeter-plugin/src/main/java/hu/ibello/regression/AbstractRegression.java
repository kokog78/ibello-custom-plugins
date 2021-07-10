package hu.ibello.regression;

import java.util.List;

import org.orangepalantir.leastsquares.Fitter;
import org.orangepalantir.leastsquares.fitters.LinearFitter;
import org.orangepalantir.leastsquares.fitters.MarquardtFitter;
import org.orangepalantir.leastsquares.fitters.NonLinearSolver;

import hu.ibello.regression.functions.Function;

abstract class AbstractRegression {
	
	protected static enum Type {
		LINEAR,
		NONLINEAR,
		MARQUARDT;
	}
	
	protected final List<DataPoint> data;

	public AbstractRegression(List<DataPoint> data) {
		super();
		this.data = data;
	}
	
	protected abstract Type getType();
	
	protected abstract Function getFunction();
	
	public void run() {
		Function function = getFunction();
		double[] parameters = getParameters(function);
		Fitter fitter;
		switch (getType()) {
		case LINEAR:
			LinearFitter linearFitter = new LinearFitter(transform(function));
			fitter = linearFitter;
			break;
		case NONLINEAR:
			NonLinearSolver nonlinearFitter = new NonLinearSolver(transform(function));
			nonlinearFitter.setStepSize(getStepSize(function));
			fitter = nonlinearFitter;
			break;
		case MARQUARDT:
			MarquardtFitter marquardtFitter = new MarquardtFitter(transform(function));
			fitter = marquardtFitter;
			break;
		default:
			throw new IllegalStateException();
		}
		double[][] xvalues = new double[data.size()][1];
		double[] zvalues = new double[data.size()];
		for (int i=0; i<data.size(); i++) {
			DataPoint point = data.get(i);
			xvalues[i][0] = point.getX();
			zvalues[i] = point.getY();
		}
		fitter.setData(xvalues, zvalues);
		fitter.setParameters(parameters);
		fitter.fitData();
		setParameters(function, fitter.getParameters());
	}
	
	private double getStepSize(Function function) {
		double stepSize = 0.0001;
		for (int i=0; i<function.getParameterCount(); i++) {
			double step = Math.abs(function.getParameter(i) / 100.0);
			if (step > 0.0) {
				stepSize = Math.min(stepSize, step);
			}
		}
		return stepSize;
	}
	
	private double[] getParameters(Function function) {
		double[] parameters = new double[function.getParameterCount()];
		for (int i=0; i<function.getParameterCount(); i++) {
			parameters[i] = function.getParameter(i);
		}
		return parameters;
	}
	
	private void setParameters(Function function, double[] parameters) {
		for (int i=0; i<function.getParameterCount(); i++) {
			function.setParameter(i, parameters[i]);
		}
	}
	
	private org.orangepalantir.leastsquares.Function transform(Function fn) {
		return new org.orangepalantir.leastsquares.Function() {
			
			@Override
			public int getNParameters() {
				return fn.getParameterCount();
			}
			
			@Override
			public int getNInputs() {
				return 1;
			}
			
			@Override
			public double evaluate(double[] values, double[] parameters) {
				for (int i=0; i<parameters.length; i++) {
					fn.setParameter(i, parameters[i]);
				}
				return fn.value(values[0]);
			}
		};
	}

}
