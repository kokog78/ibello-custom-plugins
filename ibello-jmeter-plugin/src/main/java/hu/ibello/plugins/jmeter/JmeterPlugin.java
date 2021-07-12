package hu.ibello.plugins.jmeter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hu.ibello.functions.DataPoint;
import hu.ibello.functions.ExponentialApdexFunction;
import hu.ibello.functions.ExponentialApdexInverseFunction;
import hu.ibello.functions.Function;
import hu.ibello.functions.LogisticApdexFunction;
import hu.ibello.functions.LogisticApdexInverseFunction;
import hu.ibello.graph.Graph;
import hu.ibello.plugins.IbelloTaskRunner;
import hu.ibello.plugins.PluginException;
import hu.ibello.plugins.PluginInitializer;
import hu.ibello.plugins.jmeter.model.ApdexData;
import hu.ibello.plugins.jmeter.model.ApdexFunctionType;
import hu.ibello.plugins.jmeter.model.JmeterResult;
import hu.ibello.transform.TransformerException;

public class JmeterPlugin implements IbelloTaskRunner {

	private final static String TASK_APDEX = "jmeter.apdex";
	private final static String PARAMETER_RESULT_FILE = "jmeter.file.result";
	private final static String PARAMETER_ENCODING = "jmeter.file.encoding";
	private final static String PARAMETER_THRESHOLD_SATISFIED = "jmeter.threshold.satisfied";
	private final static String PARAMETER_THRESHOLD_TOLERATED = "jmeter.threshold.tolerated";
	private final static String PARAMETER_FUNCTION = "jmeter.apdex.function";
	
	private PluginInitializer tools;
	
	@Override
	public void initialize(PluginInitializer initializer) throws PluginException {
		this.tools = initializer;
	}

	@Override
	public boolean runTask(String name) throws PluginException {
		if (name.equals(TASK_APDEX)) {
			File file = tools.getConfigurationValue(PARAMETER_RESULT_FILE).toFile();
			if (file == null) {
				tools.error("File should be specified.");
			} else {
				String encoding = tools.getConfigurationValue(PARAMETER_ENCODING).toString("UTF-8");
				int satisfactionThreshold = tools.getConfigurationValue(PARAMETER_THRESHOLD_SATISFIED).toInteger(3000);
				int tolerationThreshold = tools.getConfigurationValue(PARAMETER_THRESHOLD_TOLERATED).toInteger(12000);
				ApdexFunctionType type = tools.getConfigurationValue(PARAMETER_FUNCTION).toEnum(ApdexFunctionType.class);
				if (type == null) {
					type = ApdexFunctionType.Exponential;
				}
				List<JmeterResult> results = loadResults(file, encoding);
				List<String> labels = new ArrayList<>();
				Map<String, ApdexData> apdexMap = new HashMap<>();
				for (JmeterResult result : results) {
					if (!labels.contains(result.getLabel())) {
						labels.add(result.getLabel());
					}
					ApdexData apdex = getApdexFor(apdexMap, result);
					if (result.getElapsed() <= satisfactionThreshold) {
						apdex.satisfied(1);
					} else if (result.getElapsed() <= tolerationThreshold) {
						apdex.tolerated(1);
					} else {
						apdex.frustrated(1);
					}
				}
				printApdex(labels, apdexMap);
				List<DataPoint> points = getApdexData(apdexMap);
				Function apdexFunction = getApdexFunction(points, type);
				Function inverseFunction = getInverseApdexFunction(apdexFunction, type);
				printRequestLimits(inverseFunction, 0.8, 0.5);
				// graph
				Graph graph = tools.graph().createGraph("APDEX");
				graph.add(apdexFunction.toString(), apdexFunction);
				graph.add("Measured", points);
			}
		}
		return false;
	}

	@Override
	public void shutdown() throws PluginException {
	}
	
	private void printApdex(List<String> labels, Map<String, ApdexData> apdexMap) {
		int labelSize = 0;
		for (String label : labels) {
			labelSize = Math.max(labelSize, label.length());
		}
		labelSize = Math.max(labelSize, 5);
		tools.info("APDEX of the selected results");
		String format = "- %" + labelSize + "s: %.3f";
		ApdexData totalApdex = new ApdexData();
		for (String label : labels) {
			ApdexData apdex = apdexMap.get(label);
			print(format, label, apdex.getApdex());
			totalApdex.satisfied(apdex.getSatisfiedCount());
			totalApdex.tolerated(apdex.getToleratedCount());
			totalApdex.frustrated(apdex.getFrustratingCount());
		}
		print(format, "Total", totalApdex.getApdex());
	}
	
	private void printRequestLimits(Function apdexFunction, double limit1, double limit2) {
		long count1 = Math.round(apdexFunction.value(limit1));
		long count2 = Math.round(apdexFunction.value(limit2));
		print("Normal limit   :%d requests", count1);
		print("Overload limit :%d requests", count2);
	}
	
	private List<DataPoint> getApdexData(Map<String, ApdexData> apdexMap) {
		List<DataPoint> points = new ArrayList<>();
		for (ApdexData apdex : apdexMap.values()) {
			points.add(point(apdex.count(), apdex.getApdex()));
		}
		Collections.sort(points, (p1, p2) -> Double.compare(p1.getX(), p2.getX()));
		return points;
	}
	
	private Function getApdexFunction(List<DataPoint> points, ApdexFunctionType type) {
		Function function;
		switch (type) {
		case Logistic:
			function = getLogistic3Function(points);
			break;
		default:
			function = getExponentialApdexFunction(points);
			break;
		}
		tools.regression().getNonLinearRegression(function, points).run();
		return function;
	}
	
	private Function getInverseApdexFunction(Function function, ApdexFunctionType type) {
		Function inverse;
		switch (type) {
		case Logistic:
			inverse = new LogisticApdexInverseFunction();
			break;
		default:
			inverse = new ExponentialApdexInverseFunction();
			break;
		}
		inverse.setParameters(function.getParameters());
		return inverse;
	}

	private ApdexData getApdexFor(Map<String, ApdexData> apdexMap, JmeterResult result) {
		ApdexData apdex = apdexMap.get(result.getLabel());
		if (apdex == null) {
			apdex = new ApdexData();
			apdexMap.put(result.getLabel(), apdex);
		}
		return apdex;
	}

	private List<JmeterResult> loadResults(File file, String encoding) throws PluginException {
		List<JmeterResult> results;
		try (Reader reader = new InputStreamReader(new FileInputStream(file), encoding)) {
			results = tools.csv().fromCsv(reader, JmeterResult.class);
		} catch (IOException|TransformerException ex) {
			throw new PluginException("Cannot load Jmeter result file", ex);
		}
		return results;
	}
	
	private ExponentialApdexFunction getExponentialApdexFunction(List<DataPoint> points) {
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
	
	private LogisticApdexFunction getLogistic3Function(List<DataPoint> points) {
		LogisticApdexFunction function = new LogisticApdexFunction();
		function.setB(60);
		function.setC(10);
		function.setM(0.01);
		return function;
	}
	
	private DataPoint point(double x, double y) {
		return new DataPoint() {
			@Override
			public double getX() {
				return x;
			}

			@Override
			public double getY() {
				return y;
			}
		};
	}
	
	private void print(String format, Object ... attrs) {
		String msg = String.format(format, attrs);
		tools.info(msg);
	}

}
