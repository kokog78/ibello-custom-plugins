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

import hu.ibello.functions.ConstantFunction;
import hu.ibello.functions.CumulativeRayleighFunction;
import hu.ibello.functions.DataPoint;
import hu.ibello.functions.ExponentialApdexFunction;
import hu.ibello.functions.ExponentialApdexInverseFunction;
import hu.ibello.functions.Function;
import hu.ibello.functions.LogisticApdexFunction;
import hu.ibello.functions.LogisticApdexInverseFunction;
import hu.ibello.functions.MirrorZFunction;
import hu.ibello.functions.X0Function;
import hu.ibello.functions.ZFunction;
import hu.ibello.graph.Graph;
import hu.ibello.plugins.IbelloTaskRunner;
import hu.ibello.plugins.PluginException;
import hu.ibello.plugins.PluginInitializer;
import hu.ibello.plugins.jmeter.model.ApdexFunctionType;
import hu.ibello.plugins.jmeter.model.ConcurrentRequestData;
import hu.ibello.plugins.jmeter.model.JmeterResult;
import hu.ibello.table.Table;
import hu.ibello.table.TableRow;
import hu.ibello.transform.TransformerException;

public class JmeterPlugin implements IbelloTaskRunner {

	private final static String TASK_APDEX = "jmeter.apdex";
	private final static String PARAMETER_RESULT_FILE = "jmeter.file.result";
	private final static String PARAMETER_ENCODING = "jmeter.file.encoding";
	private final static String PARAMETER_THRESHOLD_SATISFIED = "jmeter.threshold.satisfied";
	private final static String PARAMETER_THRESHOLD_TOLERATED = "jmeter.threshold.tolerated";
	private final static String PARAMETER_FUNCTION = "jmeter.apdex.function";
	private final static String PARAMETER_APDEX_SATISFIED = "jmeter.apdex.satisfied";
	private final static String PARAMETER_APDEX_TOLERATED = "jmeter.apdex.tolerated";
	
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
				int satisfiedThresholds = tools.getConfigurationValue(PARAMETER_THRESHOLD_SATISFIED).toInteger(3000);
				int toleratedThresholds = tools.getConfigurationValue(PARAMETER_THRESHOLD_TOLERATED).toInteger(12000);
				ApdexFunctionType type = tools.getConfigurationValue(PARAMETER_FUNCTION).toEnum(ApdexFunctionType.class);
				if (type == null) {
					type = ApdexFunctionType.Exponential;
				}
				double apdexLimitSatisfied = tools.getConfigurationValue(PARAMETER_APDEX_SATISFIED).toDouble(0.8);
				double apdexLimitTolerated = tools.getConfigurationValue(PARAMETER_APDEX_TOLERATED).toDouble(0.5);
				// process results
				List<JmeterResult> results = loadResults(file, encoding);
				ConcurrentRequestData total = new ConcurrentRequestData(satisfiedThresholds, toleratedThresholds);
				List<ConcurrentRequestData> stats = getSortedStats(results, total, satisfiedThresholds, toleratedThresholds);
				// apdex
				Function inverseFunction = null;
				if (hasNon1Apdex(stats)) {
					List<DataPoint> apdexPoints = getApdexData(stats);
					Function apdexFunction = getApdexFunction(apdexPoints, type);
					inverseFunction = getInverseApdexFunction(apdexFunction, type);
					createApdexGraph(apdexPoints, apdexFunction, apdexLimitSatisfied, apdexLimitTolerated);
				}
				// failures
				double failureLimit = Double.NaN;
				int failures = getFailurePointCount(stats);
				if (failures > 0) {
					List<DataPoint> failurePoints = getFailureData(stats);
					X0Function failureFunction = getFailureFunction(failurePoints, failures);
					failureLimit = Math.max(getLastSuccessfulRequestCount(failurePoints), failureFunction.getX0());
					// failure graph
					createFailureGraph(failurePoints, failureFunction);
				}
				// summary table
				printSummary(stats, total);
				// request limits
				printRequestLimits(inverseFunction, apdexLimitSatisfied, apdexLimitTolerated, failureLimit);
				// average response times
				createResponseTimeGraph(stats);
			}
		}
		return false;
	}

	private void createApdexGraph(List<DataPoint> points, Function apdexFunction, double apdexLimitSatisfied, double apdexLimitTolerated) {
		Graph graph = tools.graph().createGraph("Application Performance Index");
		graph.setXAxis("Number of Concurrent Requests", null, null);
		graph.setYAxis("Application Performance Index");
		graph.add(apdexFunction.toString(), apdexFunction);
		graph.add("Measured", points);
		graph.add("Satistaction limit", new ConstantFunction(apdexLimitSatisfied));
		graph.add("Toleration limit", new ConstantFunction(apdexLimitTolerated));
	}
	
	private void createFailureGraph(List<DataPoint> points, Function errorFunction) {
		Graph graph = tools.graph().createGraph("Response Failures");
		graph.setXAxis("Number of Concurrent Requests", null, null);
		graph.setYAxis("Failure Ratio");
		if (errorFunction != null) {
			graph.add(errorFunction.toString(), errorFunction);
		}
		graph.add("Measured", points);
	}
	
	private void createResponseTimeGraph(List<ConcurrentRequestData> stats) {
		Graph graph = tools.graph().createGraph("Response Time");
		graph.setXAxis("Number of Concurrent Requests", null, null);
		graph.setYAxis("Response Time [ms]");
		List<DataPoint> min = new ArrayList<>();
		List<DataPoint> max = new ArrayList<>();
		List<DataPoint> avg = new ArrayList<>();
		List<DataPoint> pct90 = new ArrayList<>();
		for (ConcurrentRequestData data : stats) {
			min.add(point(data.count(), data.getMinElapsed()));
			max.add(point(data.count(), data.getMaxElapsed()));
			avg.add(point(data.count(), data.getAverageElapsed()));
			pct90.add(point(data.count(), data.get90PercentElapsed()));
		}
		graph.add("Minimum", min);
		graph.add("Average", avg);
		graph.add("90% Percentile", pct90);
		graph.add("Maximum", max);
	}

	private void printSummary(List<ConcurrentRequestData> stats, ConcurrentRequestData totalData) {
		Table table = tools.table().createTable("Statistics by Number of Concurrent Requests");
		table.getHeader().addCell("NCR");
		table.getHeader().addCell("Failures");
		table.getHeader().addCell("Failures %");
		table.getHeader().addCell("Min Response Time [ms]");
		table.getHeader().addCell("Avg Response Time [ms]");
		table.getHeader().addCell("90% Percentile [ms]");
		table.getHeader().addCell("Max Response Time [ms]");
		table.getHeader().addCell("APDEX");
		for (ConcurrentRequestData data : stats) {
			TableRow row = table.addRow();
			row.addCell(data.count());
			row.addCell(data.getFailureCount());
			row.addCell(Math.round(100 * data.getFailureRatio()));
			row.addCell(data.getMinElapsed());
			row.addCell(Math.round(data.getAverageElapsed()));
			row.addCell(Math.round(data.get90PercentElapsed()));
			row.addCell(data.getMaxElapsed());
			row.addCell(roundApdex(data.getApdex()));
		}
		TableRow row = table.addRow();
		row.addCell("Summary");
		row.addCell(totalData.getFailureCount());
		row.addCell(Math.round(100 * totalData.getFailureRatio()));
		row.addCell(totalData.getMinElapsed());
		row.addCell(Math.round(totalData.getAverageElapsed()));
		row.addCell(Math.round(totalData.get90PercentElapsed()));
		row.addCell(totalData.getMaxElapsed());
		row.addCell(roundApdex(totalData.getApdex()));
	}
	
	private void printRequestLimits(Function apdexFunction, double limit1, double limit2, double errorLimit) {
		boolean hasApdexLimits = apdexFunction != null;
		boolean hasErrorLimit = !Double.isNaN(errorLimit);
		if (hasApdexLimits || hasErrorLimit) {
			Table table = tools.table().createTable("Concurrent Request Limits");
			table.getHeader().addCell("Limit");
			table.getHeader().addCell("NCR");
			if (hasApdexLimits) {
				long count1 = Math.round(apdexFunction.value(limit1));
				count1 = Math.max(0, count1);
				long count2 = Math.round(apdexFunction.value(limit2));
				count2 = Math.max(0, count2);
				TableRow row = table.addRow();
				row.addCell("Users are satisfied until");
				row.addCell(count1);
				row = table.addRow();
				row.addCell("Users are tolerating slowness until");
				row.addCell(count2);
			}
			if (hasErrorLimit) {
				long count3 = Math.round(errorLimit);
				count3 = Math.max(0, count3);
				TableRow row = table.addRow();
				row.addCell("Responses are error-free until");
				row.addCell(count3);
			}
		}
	}
	
	private List<DataPoint> getApdexData(List<ConcurrentRequestData> stats) {
		List<DataPoint> points = new ArrayList<>();
		for (ConcurrentRequestData data : stats) {
			points.add(point(data.count(), data.getApdex()));
		}
		return points;
	}
	
	private Function getApdexFunction(List<DataPoint> points, ApdexFunctionType type) {
		Function function;
		switch (type) {
		case Linear:
			function = getZFunction(points);
			break;
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
	
	private int getFailurePointCount(List<ConcurrentRequestData> stats) {
		int result = 0;
		for (ConcurrentRequestData data : stats) {
			if (data.hasFailure()) {
				result++;
			}
		}
		return result;
	}
	
	private boolean hasNon1Apdex(List<ConcurrentRequestData> stats) {
		for (ConcurrentRequestData data : stats) {
			if (data.getApdex() < 1.0) {
				return true;
			}
		}
		return false;
	}
	
	private List<DataPoint> getFailureData(List<ConcurrentRequestData> stats) {
		List<DataPoint> points = new ArrayList<>();
		points.add(point(0.0, 0.0));
		for (ConcurrentRequestData data : stats) {
			points.add(point(data.count(), data.getFailureRatio()));
		}
		return points;
	}
	
	private double getLastSuccessfulRequestCount(List<DataPoint> points) {
		double result = 0;
		for (DataPoint point : points) {
			if (point.getY() == 0.0) {
				result = point.getX();
			} else {
				break;
			}
		}
		return result;
	}

	private X0Function getFailureFunction(List<DataPoint> points, int errors) {
		X0Function function;
		if (errors > 1) {
			function = getCumulativeRayleighFunction(points);
		} else {
			function = getMirrorZFunction(points);
		}
		tools.regression().getNonLinearRegression(function, points).run();
		return function;
	}
	
	private ConcurrentRequestData getRequestDataFor(Map<String, ConcurrentRequestData> apdexMap, JmeterResult result, int satisfiedThreshold, int toleratedThreshold) {
		ConcurrentRequestData apdex = apdexMap.get(result.getLabel());
		if (apdex == null) {
			apdex = new ConcurrentRequestData(satisfiedThreshold, toleratedThreshold);
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
	
	private CumulativeRayleighFunction getCumulativeRayleighFunction(List<DataPoint> points) {
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
	
	private MirrorZFunction getMirrorZFunction(List<DataPoint> points) {
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
	
	private ZFunction getZFunction(List<DataPoint> points) {
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
	
	private double roundApdex(double apdex) {
		return Math.round(apdex * 1000) / 1000.0;
	}
	
	private List<ConcurrentRequestData> getSortedStats(List<JmeterResult> results, ConcurrentRequestData total, int satisfiedThresholds, int toleratedThresholds) {
		Map<String, ConcurrentRequestData> map = new HashMap<>();
		for (JmeterResult result : results) {
			ConcurrentRequestData requestData = getRequestDataFor(map, result, satisfiedThresholds, toleratedThresholds);
			requestData.register(result);
			total.register(result);
		}
		List<ConcurrentRequestData> list = new ArrayList<>(map.values());
		Collections.sort(list, (data1, data2) -> data1.count() - data2.count());
		return list;
	}

	@Override
	public void shutdown() throws PluginException {
	}
	
}
