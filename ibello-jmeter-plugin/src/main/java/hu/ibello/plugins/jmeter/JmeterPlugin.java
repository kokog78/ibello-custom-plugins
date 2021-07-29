package hu.ibello.plugins.jmeter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import hu.ibello.functions.ConstantFunction;
import hu.ibello.functions.DataPoint;
import hu.ibello.functions.DataPointImpl;
import hu.ibello.functions.Function;
import hu.ibello.functions.InversableFunction;
import hu.ibello.functions.X0Function;
import hu.ibello.graph.Graph;
import hu.ibello.plugins.IbelloTaskRunner;
import hu.ibello.plugins.PluginException;
import hu.ibello.plugins.PluginInitializer;
import hu.ibello.plugins.jmeter.model.ApdexFunctionType;
import hu.ibello.plugins.jmeter.model.ConcurrentRequestData;
import hu.ibello.plugins.jmeter.model.GroupedRequestData;
import hu.ibello.plugins.jmeter.model.JmeterResult;
import hu.ibello.table.Table;
import hu.ibello.table.TableRow;
import hu.ibello.transform.TransformerException;

public class JmeterPlugin implements IbelloTaskRunner {

	private final static String TASK_BASIC = "jmeter.basic";
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
	private FunctionHelper functions;
	
	@Override
	public void initialize(PluginInitializer initializer) throws PluginException {
		this.tools = initializer;
		functions = new FunctionHelper(this.tools.regression());
	}
	
	@Override
	public boolean runTask(String name) throws PluginException {
		if (name.equals(TASK_BASIC)) {
			List<JmeterResult> results = loadResults();
			if (results != null) {
				ResultStatCollector collector = new ResultStatCollector(results);
				List<GroupedRequestData> stats = collector.getGroupedStats();
				GroupedRequestData total = collector.getTotalGroupedStats();
				printDates(results);
				tableLabels(stats, total);
			}
			return true;
		} else if (name.equals(TASK_NCR)) {
			List<JmeterResult> results = loadResults();
			if (results != null) {
				int satisfiedThresholds = tools.getConfigurationValue(PARAMETER_THRESHOLD_SATISFIED).toInteger(3000);
				int toleratedThresholds = tools.getConfigurationValue(PARAMETER_THRESHOLD_TOLERATED).toInteger(12000);
				ApdexFunctionType type = tools.getConfigurationValue(PARAMETER_FUNCTION).toEnum(ApdexFunctionType.class);
				if (type == null) {
					type = ApdexFunctionType.Exponential;
				}
				double apdexLimitSatisfied = tools.getConfigurationValue(PARAMETER_APDEX_SATISFIED).toDouble(0.9);
				double apdexLimitTolerated = tools.getConfigurationValue(PARAMETER_APDEX_TOLERATED).toDouble(0.5);
				ResultStatCollector collector = new ResultStatCollector(results);
				List<ConcurrentRequestData> stats = collector.getConcurrentStats(satisfiedThresholds, toleratedThresholds);
				ConcurrentRequestData total = collector.getTotalConcurrentStats();
				// test run info
				printDates(results);
				// apdex
				Function apdexInverseFunction = processApdex(stats, type, apdexLimitSatisfied, apdexLimitTolerated);
				// failures
				FailureData failure = processFailures(stats);
				// summary table
				tableSummary(stats, total);
				// request limits
				tableRequestLimits(apdexInverseFunction, apdexLimitSatisfied, apdexLimitTolerated, failure);
				// throughput
				processThroughput(stats, total, failure);
				// average response times
				createResponseTimeGraph(stats);
			}
			return true;
		}
		return false;
	}

	private List<JmeterResult> loadResults() throws PluginException {
		File file = tools.getConfigurationValue(PARAMETER_RESULT_FILE).toFile();
		if (file == null) {
			tools.error("File should be specified.");
			return null;
		} else {
			String encoding = tools.getConfigurationValue(PARAMETER_ENCODING).toString("UTF-8");
			String keepPattern = tools.getConfigurationValue(PARAMETER_PATTERN_KEEP).toString("");
			String skipPattern = tools.getConfigurationValue(PARAMETER_PATTERN_SKIP).toString("");
			return loadResults(file, encoding, keepPattern, skipPattern);
		}
	}

	private Function processApdex(List<ConcurrentRequestData> stats, ApdexFunctionType type, double apdexLimitSatisfied, double apdexLimitTolerated) {
		Function apdexInverseFunction = null;
		if (hasNon1Apdex(stats)) {
			List<DataPoint> apdexPoints = getApdexData(stats);
			InversableFunction apdexFunction = getApdexFunction(apdexPoints, type);
			if (apdexFunction != null) {
				apdexInverseFunction = apdexFunction.getInverseFunction();
			}
			createApdexGraph(apdexPoints, apdexFunction, apdexLimitSatisfied, apdexLimitTolerated);
			printFitResult("APDEX function", apdexFunction, apdexPoints);
		}
		return apdexInverseFunction;
	}

	private FailureData processFailures(List<ConcurrentRequestData> stats) {
		FailureData failure = new FailureData();
		int failures = getFailurePointCount(stats);
		if (failures > 0) {
			List<DataPoint> failurePoints = getFailureData(stats);
			failure.updateFailureLimit(getLastSuccessfulRequestCount(failurePoints));
			failure.updateCrashLimit(getFirstFullFailureRequestCount(failurePoints));
			X0Function failureFunction = getFailureFunction(failurePoints, failures);
			if (failureFunction != null) {
				failure.updateFailureLimit(failureFunction.getX0());
				Function failureInverseFunction = failureFunction.getInverseFunction();
				failure.updateCrashLimit(failureInverseFunction.value(1.0));
			}
			// failure graph
			createFailureGraph(failurePoints, failureFunction);
			printFitResult("Failure function", failureFunction, failurePoints);
		}
		return failure;
	}

	private void processThroughput(List<ConcurrentRequestData> stats, ConcurrentRequestData total, FailureData failure) {
		tableThroughput(stats, total);
		// throughput graph
		List<DataPoint> throughputData = getThroughputData(stats);
		Function throughputFunction = getThroughputFunction(throughputData);
		createThroughputGraph(throughputData, throughputFunction);
		// sent graph
		List<DataPoint> networkSentData = getNetworkSentData(stats);
		Function networkSentFunction = getThroughputFunction(networkSentData);
		createNetworkTrafficGraph(networkSentData, networkSentFunction, "Sent");
		// received graph
		List<DataPoint> networkReceivedData = getNetworkReceivedData(stats);
		Function networkReceivedFunction = getThroughputFunction(networkReceivedData);
		createNetworkTrafficGraph(networkReceivedData, networkReceivedFunction, "Received");
		// results
		printFitResult("Throughput function", throughputFunction, throughputData);
		printMaxThroughput(throughputFunction, networkSentFunction, networkReceivedFunction, failure);
	}

	private void createApdexGraph(List<DataPoint> points, Function apdexFunction, double apdexLimitSatisfied, double apdexLimitTolerated) {
		Graph graph = tools.graph().createGraph("Application Performance Index");
		graph.setXAxis("Number of Concurrent Requests", null, null);
		graph.setYAxis("Application Performance Index");
		if (apdexFunction != null) {
			graph.add(apdexFunction.toString(), apdexFunction);
		}
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
	
	private void createThroughputGraph(List<DataPoint> points, Function function) {
		Graph graph = tools.graph().createGraph("Throughput");
		graph.setXAxis("Number of Concurrent Requests", null, null);
		graph.setYAxis("Transactions / s");
		if (function != null) {
			graph.add(function.toString(), function);
		}
		graph.add("Measured", points);
	}
	
	private void createNetworkTrafficGraph(List<DataPoint> points, Function function, String label) {
		Graph graph = tools.graph().createGraph("Network Traffic " + label);
		graph.setXAxis("Number of Concurrent Requests", null, null);
		graph.setYAxis("Network Traffic [KB / s]");
		if (function != null) {
			graph.add(function.toString(), function);
		}
		graph.add(label, points);
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
	
	private void printDates(List<JmeterResult> results) {
		Date min = null;
		Date max = null;
		for (JmeterResult result : results) {
			Date startDate = result.getStartDate();
			if (min == null || min.after(startDate)) {
				min = startDate;
			}
			Date endDate = result.getEndDate();
			if (max == null || min.before(endDate)) {
				max = endDate;
			}
		}
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String smin = min == null ? "" : fmt.format(min);
		String smax = max == null ? "" : fmt.format(max);
		if (min != null && max != null) {
			String day = smin.substring(0, 10);
			if (smax.startsWith(day)) {
				smax = smax.substring(11);
			}
		}
		print("Test run time: %s - %s", smin, smax);
	}
	
	
	private void printFitResult(String name, Function function, List<DataPoint> points) {
		if (function != null) {
			double r2 = functions.calculateR2(function, points);
			if (!Double.isNaN(r2)) {
				print("%s R2: %.2f", name, r2);
			}
		}
	}
	
	private void printMaxThroughput(Function throughputFunction, Function networkSentFunction, Function networkReceivedFunction, FailureData failure) {
		if (failure.hasFailureLimit()) {
			if (throughputFunction != null) {
				double max = throughputFunction.value(failure.getFailureLimit());
				if (!Double.isNaN(max) && Double.isFinite(max)) {
					print("Maximum throughput: %.2f transaction/s", max);
				}
			}
			if (networkSentFunction != null) {
				double max = networkSentFunction.value(failure.getFailureLimit());
				if (!Double.isNaN(max) && Double.isFinite(max)) {
					print("Maximum network traffic sent: %.2f KB/s", max);
				}
			}
			if (networkReceivedFunction != null) {
				double max = networkReceivedFunction.value(failure.getFailureLimit());
				if (!Double.isNaN(max) && Double.isFinite(max)) {
					print("Maximum network traffic reveived: %.2f KB/s", max);
				}
			}
		}
	}

	private void tableSummary(List<ConcurrentRequestData> stats, ConcurrentRequestData totalData) {
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
		row.addCell("Total");
		row.addCell(totalData.getFailureCount());
		row.addCell(Math.round(100 * totalData.getFailureRatio()));
		row.addCell(totalData.getMinElapsed());
		row.addCell(Math.round(totalData.getAverageElapsed()));
		row.addCell(Math.round(totalData.get90PercentElapsed()));
		row.addCell(totalData.getMaxElapsed());
		row.addCell(roundApdex(totalData.getApdex()));
	}
	
	private void tableRequestLimits(Function apdexFunction, double limit1, double limit2, FailureData failure) {
		boolean hasApdexLimits = apdexFunction != null;
		boolean hasErrorLimit = failure.hasFailureLimit();
		boolean hasCrashLimit = failure.hasCrashLimit();
		if (hasApdexLimits || hasErrorLimit || hasCrashLimit) {
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
				long count3 = Math.round(failure.getFailureLimit());
				count3 = Math.max(0, count3);
				TableRow row = table.addRow();
				row.addCell("Responses are error-free until");
				row.addCell(count3);
			}
			if (hasCrashLimit) {
				long count4 = Math.round(failure.getCrashLimit());
				count4 = Math.max(0, count4);
				TableRow row = table.addRow();
				row.addCell("Application crashes after");
				row.addCell(count4);
			}
		}
	}
	
	private void tableThroughput(List<ConcurrentRequestData> stats, ConcurrentRequestData totalData) {
		Table table = tools.table().createTable("Throughput");
		table.getHeader().addCell("NCR");
		table.getHeader().addCell("Transactions/s");
		table.getHeader().addCell("Sent KB/s");
		table.getHeader().addCell("Received KB/s");
		for (ConcurrentRequestData data : stats) {
			TableRow row = table.addRow();
			row.addCell(data.count());
			row.addCell(roundThroughput(data.getThroughput()));
			row.addCell(roundThroughput(data.getNetworkSent()));
			row.addCell(roundThroughput(data.getNetworkReceived()));
		}
		TableRow row = table.addRow();
		row.addCell("Total");
		row.addCell(roundThroughput(totalData.getThroughput()));
		row.addCell(roundThroughput(totalData.getNetworkSent()));
		row.addCell(roundThroughput(totalData.getNetworkReceived()));
	}
	
	private void tableLabels(List<GroupedRequestData> stats, GroupedRequestData total) {
		Table table = tools.table().createTable("Stats by Labels");
		table.getHeader().addCell("Label");
		table.getHeader().addCell("Start");
		table.getHeader().addCell("End");
		table.getHeader().addCell("Count");
		table.getHeader().addCell("Failures");
		table.getHeader().addCell("First success");
		table.getHeader().addCell("Last success");
		table.getHeader().addCell("First failed");
		table.getHeader().addCell("Last failed");
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		for (GroupedRequestData data: stats) {
			TableRow row = table.addRow();
			row.addCell(data.getLabel());
			row.addCell(fmt.format(data.getStartDate()));
			row.addCell(fmt.format(data.getEndDate()));
			row.addCell(data.getCount());
			row.addCell(data.getFailedCount());
			row.addCell(data.getFirstSuccessDate() == null ? "" : fmt.format(data.getFirstSuccessDate()));
			row.addCell(data.getLastSuccessDate() == null ? "" : fmt.format(data.getLastSuccessDate()));
			row.addCell(data.getFirstFailedDate() == null ? "" : fmt.format(data.getFirstFailedDate()));
			row.addCell(data.getLastFailedDate() == null ? "" : fmt.format(data.getLastFailedDate()));
		}
		TableRow row = table.addRow();
		row.addCell("Total");
		row.addCell(fmt.format(total.getStartDate()));
		row.addCell(fmt.format(total.getEndDate()));
		row.addCell(total.getCount());
		row.addCell(total.getFailedCount());
		row.addCell(total.getFirstSuccessDate() == null ? "" : fmt.format(total.getFirstSuccessDate()));
		row.addCell(total.getLastSuccessDate() == null ? "" : fmt.format(total.getLastSuccessDate()));
		row.addCell(total.getFirstFailedDate() == null ? "" : fmt.format(total.getFirstFailedDate()));
		row.addCell(total.getLastFailedDate() == null ? "" : fmt.format(total.getLastFailedDate()));
	}
	
	private List<DataPoint> getApdexData(List<ConcurrentRequestData> stats) {
		List<DataPoint> points = new ArrayList<>();
		for (ConcurrentRequestData data : stats) {
			points.add(point(data.count(), data.getApdex()));
		}
		return points;
	}
	
	private InversableFunction getApdexFunction(List<DataPoint> points, ApdexFunctionType type) {
		InversableFunction function;
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
		try {
			tools.regression().getNonLinearRegression(function, points).run();
		} catch (Exception ex) {
			tools.error("Cannot fit APDEX function", ex);
			function = null;
		}
		return function;
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
	
	private List<DataPoint> getThroughputData(List<ConcurrentRequestData> stats) {
		List<DataPoint> points = new ArrayList<>();
		for (ConcurrentRequestData data : stats) {
			if (!data.hasFailure()) {
				points.add(point(data.count(), data.getThroughput()));
			}
		}
		return points;
	}
	
	private List<DataPoint> getNetworkSentData(List<ConcurrentRequestData> stats) {
		List<DataPoint> points = new ArrayList<>();
		for (ConcurrentRequestData data : stats) {
			if (!data.hasFailure()) {
				points.add(point(data.count(), data.getNetworkSent()));
			}
		}
		return points;
	}
	
	private List<DataPoint> getNetworkReceivedData(List<ConcurrentRequestData> stats) {
		List<DataPoint> points = new ArrayList<>();
		for (ConcurrentRequestData data : stats) {
			if (!data.hasFailure()) {
				points.add(point(data.count(), data.getNetworkReceived()));
			}
		}
		return points;
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
	
	private double getFirstFullFailureRequestCount(List<DataPoint> points) {
		for (DataPoint point : points) {
			if (point.getY() >= 1.0) {
				return point.getX();
			}
		}
		return Double.NaN;
	}

	private X0Function getFailureFunction(List<DataPoint> points, int errors) {
		X0Function function = null;
		if (errors > 1) {
			function = functions.getLogisticErrorFunction(points);
		} else {
			function = functions.getMirrorZFunction(points);
		}
		try {
			tools.regression().getNonLinearRegression(function, points).run();
		} catch (Exception ex) {
			function = null;
			tools.error("Cannot fit failure function", ex);
		}
		return function;
	}
	
	private Function getThroughputFunction(List<DataPoint> points) {
		if (points.size() < 4) {
			return null;
		}
		Function function = functions.getLogisticThroughputFunction(points);
		tools.regression().getNonLinearRegression(function, points).run();
		return function;
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
			List<JmeterResult> oldResults = results;
			results = new ArrayList<>();
			for (JmeterResult result : oldResults) {
				if (keepP != null && !keepP.matcher(result.getLabel()).find()) {
					continue;
				}
				if (skipP != null && skipP.matcher(result.getLabel()).find()) {
					continue;
				}
				results.add(result);
			}
		}
		return results;
	}
	
	private DataPoint point(double x, double y) {
		return new DataPointImpl(x, y);
	}
	
	private double roundApdex(double apdex) {
		return Math.round(apdex * 1000) / 1000.0;
	}
	
	private double roundThroughput(double value) {
		return Math.round(value * 100) / 100.0;
	}
	
	private void print(String template, Object ... params) {
		tools.info(String.format(template, params));
	}

	@Override
	public void shutdown() throws PluginException {
	}
	
}
