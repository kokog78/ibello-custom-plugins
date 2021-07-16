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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import hu.ibello.functions.ConstantFunction;
import hu.ibello.functions.DataPoint;
import hu.ibello.functions.ExponentialApdexInverseFunction;
import hu.ibello.functions.Function;
import hu.ibello.functions.Logistic4InverseFunction;
import hu.ibello.functions.X0Function;
import hu.ibello.functions.ZInverseFunction;
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

	private final static String TASK_NCR = "jmeter.ncr";
	private final static String PARAMETER_RESULT_FILE = "jmeter.file.result";
	private final static String PARAMETER_ENCODING = "jmeter.file.encoding";
	private final static String PARAMETER_THRESHOLD_SATISFIED = "jmeter.threshold.satisfied";
	private final static String PARAMETER_THRESHOLD_TOLERATED = "jmeter.threshold.tolerated";
	private final static String PARAMETER_PATTERN_KEEP = "jmeter.pattern.keep";
	private final static String PARAMETER_PATTERN_SKIP = "jmeter.pattern.skip";
	private final static String PARAMETER_FUNCTION = "jmeter.apdex.function";
	private final static String PARAMETER_APDEX_SATISFIED = "jmeter.apdex.satisfied";
	private final static String PARAMETER_APDEX_TOLERATED = "jmeter.apdex.tolerated";
	
	private PluginInitializer tools;
	private final FunctionHelper functions = new FunctionHelper();
	
	@Override
	public void initialize(PluginInitializer initializer) throws PluginException {
		this.tools = initializer;
	}

	@Override
	public boolean runTask(String name) throws PluginException {
		if (name.equals(TASK_NCR)) {
			File file = tools.getConfigurationValue(PARAMETER_RESULT_FILE).toFile();
			if (file == null) {
				tools.error("File should be specified.");
			} else {
				String encoding = tools.getConfigurationValue(PARAMETER_ENCODING).toString("UTF-8");
				String keepPattern = tools.getConfigurationValue(PARAMETER_PATTERN_KEEP).toString("");
				String skipPattern = tools.getConfigurationValue(PARAMETER_PATTERN_SKIP).toString("");
				int satisfiedThresholds = tools.getConfigurationValue(PARAMETER_THRESHOLD_SATISFIED).toInteger(3000);
				int toleratedThresholds = tools.getConfigurationValue(PARAMETER_THRESHOLD_TOLERATED).toInteger(12000);
				ApdexFunctionType type = tools.getConfigurationValue(PARAMETER_FUNCTION).toEnum(ApdexFunctionType.class);
				if (type == null) {
					type = ApdexFunctionType.Exponential;
				}
				double apdexLimitSatisfied = tools.getConfigurationValue(PARAMETER_APDEX_SATISFIED).toDouble(0.8);
				double apdexLimitTolerated = tools.getConfigurationValue(PARAMETER_APDEX_TOLERATED).toDouble(0.5);
				// process results
				List<JmeterResult> results = loadResults(file, encoding, keepPattern, skipPattern);
				ConcurrentRequestData total = new ConcurrentRequestData(satisfiedThresholds, toleratedThresholds);
				List<ConcurrentRequestData> stats = getSortedStats(results, total, satisfiedThresholds, toleratedThresholds);
				// apdex
				Function inverseFunction = null;
				if (hasNon1Apdex(stats)) {
					List<DataPoint> apdexPoints = getApdexData(stats);
					Function apdexFunction = getApdexFunction(apdexPoints, type);
					inverseFunction = getInverseApdexFunction(apdexFunction, type);
					createApdexGraph(apdexPoints, apdexFunction, apdexLimitSatisfied, apdexLimitTolerated);
					printApdexFitResults(apdexFunction, apdexPoints);
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
	
	private void printApdexFitResults(Function apdexFunction, List<DataPoint> points) {
		double r2 = functions.calculareR2(apdexFunction, points);
		print("APDEX function R2: %.2f", r2);
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
			function = functions.getZFunction(points);
			break;
		case Logistic:
			function = functions.getLogisticApdexFunction(points);
			break;
		default:
			function = functions.getExponentialApdexFunction(points);
			break;
		}
		tools.regression().getNonLinearRegression(function, points).run();
		return function;
	}
	
	private Function getInverseApdexFunction(Function function, ApdexFunctionType type) {
		Function inverse;
		switch (type) {
		case Linear:
			inverse = new ZInverseFunction();
			break;
		case Logistic:
			inverse = new Logistic4InverseFunction();
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
			function = functions.getLogisticErrorFunction(points);
		} else {
			function = functions.getMirrorZFunction(points);
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

	private List<JmeterResult> loadResults(File file, String encoding, String keepPattern, String skipPattern) throws PluginException {
		List<JmeterResult> results;
		try (Reader reader = new InputStreamReader(new FileInputStream(file), encoding)) {
			results = tools.csv().fromCsv(reader, JmeterResult.class);
		} catch (IOException|TransformerException ex) {
			throw new PluginException("Cannot load Jmeter result file", ex);
		}
		Pattern keepP = (keepPattern != null && !keepPattern.isEmpty()) ? Pattern.compile(keepPattern) : null;
		Pattern skipP = (skipPattern != null && !skipPattern.isEmpty()) ? Pattern.compile(skipPattern) : null;
		if (keepP != null || skipP != null) {
			Stream<JmeterResult> stream = results.stream();
			if (keepP != null) {
				stream = stream.filter(r -> keepP.matcher(r.getLabel()).find());
			}
			if (skipP != null) {
				stream = stream.filter(r -> !skipP.matcher(r.getLabel()).find());
			}
			results = stream.collect(Collectors.toList());
		}
		return results;
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
	
	private void print(String template, Object ... params) {
		tools.info(String.format(template, params));
	}

	@Override
	public void shutdown() throws PluginException {
	}
	
}
