package hu.ibello.plugins.jmeter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import hu.ibello.functions.ConstantFunction;
import hu.ibello.functions.DataPoint;
import hu.ibello.functions.Function;
import hu.ibello.functions.InversableFunction;
import hu.ibello.functions.X0Function;
import hu.ibello.functions.impl.DataPointImpl;
import hu.ibello.graph.Graph;
import hu.ibello.plugins.IbelloTaskRunner;
import hu.ibello.plugins.PluginException;
import hu.ibello.plugins.PluginInitializer;
import hu.ibello.plugins.jmeter.model.ApdexFunctionType;
import hu.ibello.plugins.jmeter.model.ConcurrentRequestData;
import hu.ibello.plugins.jmeter.model.FunctionType;
import hu.ibello.plugins.jmeter.model.GroupedRequestData;
import hu.ibello.plugins.jmeter.model.JmeterResult;
import hu.ibello.table.Table;
import hu.ibello.table.TableRow;
import hu.ibello.transform.TransformerException;

public class JmeterPlugin implements IbelloTaskRunner {

	private final static String TASK_BASIC = "jmeter.basic";
	private final static String TASK_NCR = "jmeter.ncr";
	private final static String TASK_FIT = "jmeter.fit";
	private final static String PARAMETER_RESULT_FILE = "jmeter.file.result";
	private final static String PARAMETER_ENCODING = "jmeter.file.encoding";
	private final static String PARAMETER_THRESHOLD_SATISFIED = "jmeter.threshold.satisfied";
	private final static String PARAMETER_THRESHOLD_TOLERATED = "jmeter.threshold.tolerated";
	private final static String PARAMETER_PATTERN_KEEP = "jmeter.pattern.keep";
	private final static String PARAMETER_PATTERN_SKIP = "jmeter.pattern.skip";
	private final static String PARAMETER_APDEX_FUNCTION = "jmeter.apdex.function";
	private final static String PARAMETER_APDEX_SATISFIED = "jmeter.apdex.satisfied";
	private final static String PARAMETER_APDEX_TOLERATED = "jmeter.apdex.tolerated";
	private final static String PARAMETER_CSV_FILE = "jmeter.file.csv";
	private final static String PARAMETER_GRAPH_TITLE = "jmeter.title.graph";
	private final static String PARAMETER_X_TITLE = "jmeter.title.x";
	private final static String PARAMETER_Y_TITLE = "jmeter.title.y";
	private final static String PARAMETER_FIT_FUNCTION = "jmeter.fit.function";
	private final static String PARAMETER_VALUE_X = "jmeter.value.x";
	private final static String PARAMETER_VALUE_Y = "jmeter.value.y";
	
	private PluginInitializer tools;
	private FunctionHelper functions;
	private ResourceBundle resources;
	
	@Override
	public void initialize(PluginInitializer initializer) throws PluginException {
		this.tools = initializer;
		functions = new FunctionHelper(this.tools.regression());
	}
	
	@Override
	public boolean runTask(String name) throws PluginException {
		resources = ResourceBundle.getBundle(getClass().getCanonicalName(), Locale.getDefault());
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
				ApdexFunctionType type = tools.getConfigurationValue(PARAMETER_APDEX_FUNCTION).toEnum(ApdexFunctionType.class);
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
				Function successfulApdexInverseFunction = processApdex(stats, type, true, apdexLimitSatisfied, apdexLimitTolerated);
				processApdex(stats, type, false, apdexLimitSatisfied, apdexLimitTolerated);
				// failures
				FailureData failure = processFailures(stats);
				// summary table
				tableSummary(stats, total);
				// request limits
				tableRequestLimits(successfulApdexInverseFunction, apdexLimitSatisfied, apdexLimitTolerated, failure);
				// throughput
				processThroughput(stats, total, failure);
				// average response times
				createResponseTimeGraph(stats);
			}
			return true;
		} else if (name.equals(TASK_FIT)) {
			List<DataPoint> points = loadDataset();
			if (points != null) {
				FunctionType type = tools.getConfigurationValue(PARAMETER_FIT_FUNCTION).toEnum(FunctionType.class);
				if (type == null) {
					type = FunctionType.LinearApdex;
				}
				Function function = getDatasetFunction(type, points);
				try {
					tools.regression().getNonLinearRegression(function, points).run();
				} catch (Exception ex) {
					tools.error(text("cannotFitFunction"), ex);
					function = null;
				}
				String title = tools.getConfigurationValue(PARAMETER_GRAPH_TITLE).toString(text("dataset"));
				String xTitle = tools.getConfigurationValue(PARAMETER_X_TITLE).toString("X");
				String yTitle = tools.getConfigurationValue(PARAMETER_Y_TITLE).toString("Y");
				createDatasetGraph(title, xTitle, yTitle, points, function);
				if (function != null) {
					printFitResult(text("function"), function, points);
					Double x = tools.getConfigurationValue(PARAMETER_VALUE_X).toDouble();
					Double y = tools.getConfigurationValue(PARAMETER_VALUE_Y).toDouble();
					if (x != null) {
						Double value = function.value(x);
						printValues("F", x, value);
					}
					if (y != null && (function instanceof InversableFunction)) {
						Function inverse = ((InversableFunction)function).getInverseFunction();
						Double value = inverse.value(y);
						printValues("F", value, y);
					}
				}
			}
			return true;
		}
		return false;
	}

	private List<JmeterResult> loadResults() throws PluginException {
		File file = tools.getConfigurationValue(PARAMETER_RESULT_FILE).toFile();
		if (file == null) {
			tools.error(text("fileShouldBeSpecified"));
			return null;
		} else {
			String encoding = tools.getConfigurationValue(PARAMETER_ENCODING).toString("UTF-8");
			String keepPattern = tools.getConfigurationValue(PARAMETER_PATTERN_KEEP).toString("");
			String skipPattern = tools.getConfigurationValue(PARAMETER_PATTERN_SKIP).toString("");
			return loadResults(file, encoding, keepPattern, skipPattern);
		}
	}
	
	private List<DataPoint> loadDataset() throws PluginException {
		File file = tools.getConfigurationValue(PARAMETER_CSV_FILE).toFile();
		if (file == null) {
			tools.error(text("fileShouldBeSpecified"));
			return null;
		} else {
			return loadDataset(file);
		}
	}
	
	private Function getDatasetFunction(FunctionType type, List<? extends DataPoint> points) {
		switch (type) {
		case LinearApdex:
			return functions.getZFunction(points);
		case ExponentialApdex:
			return functions.getExponentialApdexFunction(points);
		case LogisticApdex:
			return functions.getLogisticApdexFunction(points);
		case LinearError:
			return functions.getMirrorZFunction(points);
		case LogisticError:
			return functions.getLogisticErrorFunction(points);
		case LogisticThroughput:
			return functions.getLogisticThroughputFunction(points);
		case CumulativeRayleigh:
			return functions.getCumulativeRayleighFunction(points);
		case ExponentialDistribution:
			return functions.getExponentialDistributionFunction(points);
		}
		return null;
	}

	private Function processApdex(List<ConcurrentRequestData> stats, ApdexFunctionType type, boolean successful, double apdexLimitSatisfied, double apdexLimitTolerated) {
		Function apdexInverseFunction = null;
		if (hasNon1Apdex(stats, successful)) {
			List<DataPoint> apdexPoints = successful ? getSuccessfulApdexData(stats) : getApdexData(stats);
			InversableFunction apdexFunction = getApdexFunction(apdexPoints, type);
			if (apdexFunction != null) {
				apdexInverseFunction = apdexFunction.getInverseFunction();
			}
			createApdexGraph(apdexPoints, apdexFunction, successful, apdexLimitSatisfied, apdexLimitTolerated);
			printFitResult(text("apdexFunction"), apdexFunction, apdexPoints);
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
			printFitResult(text("failureFunction"), failureFunction, failurePoints);
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
		createNetworkTrafficGraph(networkSentData, networkSentFunction, text("sent"));
		// received graph
		List<DataPoint> networkReceivedData = getNetworkReceivedData(stats);
		Function networkReceivedFunction = getThroughputFunction(networkReceivedData);
		createNetworkTrafficGraph(networkReceivedData, networkReceivedFunction, text("received"));
		// results
		printFitResult(text("throughputFunction"), throughputFunction, throughputData);
		printMaxThroughput(throughputFunction, networkSentFunction, networkReceivedFunction, failure);
	}

	private void createApdexGraph(List<DataPoint> points, Function apdexFunction, boolean successful, double apdexLimitSatisfied, double apdexLimitTolerated) {
		String title = text(successful ? "applicationPerformanceIndexSuccessful" : "applicationPerformanceIndex");
		Graph graph = tools.graph().createGraph(title);
		graph.setXAxis(text("numberConcurrentRequests"), null, null);
		graph.setYAxis(text("applicationPerformanceIndex"));
		if (apdexFunction != null) {
			graph.add(apdexFunction.toString(), apdexFunction);
		}
		graph.add(text("measured"), points);
		graph.add(text("satisfactionValue"), new ConstantFunction(apdexLimitSatisfied));
		graph.add(text("tolerationValue"), new ConstantFunction(apdexLimitTolerated));
	}
	
	private void createFailureGraph(List<DataPoint> points, Function errorFunction) {
		Graph graph = tools.graph().createGraph(text("responseFailures"));
		graph.setXAxis(text("numberConcurrentRequests"), null, null);
		graph.setYAxis(text("failureRatio"));
		if (errorFunction != null) {
			graph.add(errorFunction.toString(), errorFunction);
		}
		graph.add(text("measured"), points);
	}
	
	private void createThroughputGraph(List<DataPoint> points, Function function) {
		Graph graph = tools.graph().createGraph(text("throughput"));
		graph.setXAxis(text("numberConcurrentRequests"), null, null);
		graph.setYAxis(text("transactions.units"));
		if (function != null) {
			graph.add(function.toString(), function);
		}
		graph.add(text("measured"), points);
	}
	
	private void createNetworkTrafficGraph(List<DataPoint> points, Function function, String label) {
		Graph graph = tools.graph().createGraph(text("networkTraffic") + " " + label);
		graph.setXAxis(text("numberConcurrentRequests"), null, null);
		graph.setYAxis(text("networkTraffic.units"));
		if (function != null) {
			graph.add(function.toString(), function);
		}
		graph.add(label, points);
	}
	
	private void createResponseTimeGraph(List<ConcurrentRequestData> stats) {
		Graph graph = tools.graph().createGraph(text("responseTime"));
		graph.setXAxis(text("numberConcurrentRequests"), null, null);
		graph.setYAxis(text("responseTime.units"));
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
		graph.add(text("minimum"), min);
		graph.add(text("average"), avg);
		graph.add(text("percentile.90"), pct90);
		graph.add(text("maximum"), max);
	}
	
	private void createDatasetGraph(String title, String xTitle, String yTitle, List<DataPoint> points, Function function) {
		Graph graph = tools.graph().createGraph(title);
		graph.setXAxis(xTitle, null, null);
		graph.setYAxis(yTitle);
		if (function != null) {
			graph.add(function.toString(), function);
		}
		graph.add(text("points"), points);
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
		print(text("testRunTime") + ": %s - %s", smin, smax);
	}
	
	
	private void printFitResult(String name, Function function, List<DataPoint> points) {
		if (function != null) {
			double r2 = functions.calculateR2(function, points);
			if (!Double.isNaN(r2)) {
				print("%s R2: %.2f", name, r2);
			}
		}
	}
	
	private void printValues(String functionName, Double x, Double y) {
		if (x != null && y != null) {
			print("%s(%.2f) = %.2f", functionName, x, y);
		} else if (x != null) {
			print("%s(%.2f) = ?", functionName, x);
		} else if (y != null) {
			print("%s(?) = %.2f", functionName, y);
		} else {
			print("%s(?) = ?", functionName);
		}
	}
	
	private void printMaxThroughput(Function throughputFunction, Function networkSentFunction, Function networkReceivedFunction, FailureData failure) {
		if (failure.hasFailureLimit()) {
			if (throughputFunction != null) {
				double max = throughputFunction.value(failure.getFailureLimit());
				if (!Double.isNaN(max) && Double.isFinite(max)) {
					print(text("maximumThroughput") + ": %.2f " + text("transactions.units"), max);
				}
			}
			if (networkSentFunction != null) {
				double max = networkSentFunction.value(failure.getFailureLimit());
				if (!Double.isNaN(max) && Double.isFinite(max)) {
					print(text("maximumNetworkTrafficSent") + ": %.2f KB/s", max);
				}
			}
			if (networkReceivedFunction != null) {
				double max = networkReceivedFunction.value(failure.getFailureLimit());
				if (!Double.isNaN(max) && Double.isFinite(max)) {
					print(text("maximumNetworkTrafficReceived") + ": %.2f KB/s", max);
				}
			}
		}
	}

	private void tableSummary(List<ConcurrentRequestData> stats, ConcurrentRequestData totalData) {
		Table table = tools.table().createTable(text("statsByNCR"));
		table.getHeader().addCell("NCR");
		table.getHeader().addCell(text("failures"));
		table.getHeader().addCell(text("failures.percent"));
		table.getHeader().addCell(text("minResponseTime"));
		table.getHeader().addCell(text("avgResponseTime"));
		table.getHeader().addCell(text("percentile90ResponseTime"));
		table.getHeader().addCell(text("maxResponseTime"));
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
			row.addCell(roundApdex(data.getSuccessfulApdex()));
		}
		TableRow row = table.addRow();
		row.addCell(text("total"));
		row.addCell(totalData.getFailureCount());
		row.addCell(Math.round(100 * totalData.getFailureRatio()));
		row.addCell(totalData.getMinElapsed());
		row.addCell(Math.round(totalData.getAverageElapsed()));
		row.addCell(Math.round(totalData.get90PercentElapsed()));
		row.addCell(totalData.getMaxElapsed());
		row.addCell(roundApdex(totalData.getSuccessfulApdex()));
	}
	
	private void tableRequestLimits(Function apdexFunction, double limit1, double limit2, FailureData failure) {
		boolean hasApdexLimits = apdexFunction != null;
		boolean hasErrorLimit = failure.hasFailureLimit();
		boolean hasCrashLimit = failure.hasCrashLimit();
		if (hasApdexLimits || hasErrorLimit || hasCrashLimit) {
			Table table = tools.table().createTable(text("concurrentRequestLimits"));
			table.getHeader().addCell(text("limit"));
			table.getHeader().addCell("NCR");
			if (hasApdexLimits) {
				double count1 = apdexFunction.value(limit1);
				addLimitToTable(table, text("satisfactionLimit"), count1);
				double count2 = apdexFunction.value(limit2);
				addLimitToTable(table, text("tolerationLimit"), count2);
			}
			addLimitToTable(table, text("stabilityLimit"), failure.getFailureLimit());
			addLimitToTable(table, text("overloadLimit"), failure.getCrashLimit());
		}
	}
	
	private void addLimitToTable(Table table, String text, double limit) {
		if (!Double.isNaN(limit) && Double.isFinite(limit)) {
			limit = Math.max(0, limit);
			TableRow row = table.addRow();
			row.addCell(text);
			row.addCell(Math.round(limit));
		}
	}
	
	private void tableThroughput(List<ConcurrentRequestData> stats, ConcurrentRequestData totalData) {
		Table table = tools.table().createTable(text("throughput"));
		table.getHeader().addCell("NCR");
		table.getHeader().addCell(text("transactions.units"));
		table.getHeader().addCell(text("sent.units"));
		table.getHeader().addCell(text("received.units"));
		for (ConcurrentRequestData data : stats) {
			TableRow row = table.addRow();
			row.addCell(data.count());
			row.addCell(roundThroughput(data.getThroughput()));
			row.addCell(roundThroughput(data.getNetworkSent()));
			row.addCell(roundThroughput(data.getNetworkReceived()));
		}
		TableRow row = table.addRow();
		row.addCell(text("total"));
		row.addCell(roundThroughput(totalData.getThroughput()));
		row.addCell(roundThroughput(totalData.getNetworkSent()));
		row.addCell(roundThroughput(totalData.getNetworkReceived()));
	}
	
	private void tableLabels(List<GroupedRequestData> stats, GroupedRequestData total) {
		Collections.sort(stats, getStatComparator());
		Table table = tools.table().createTable(text("statsByLabels"));
		table.getHeader().addCell(text("label"));
		table.getHeader().addCell(text("start"));
		table.getHeader().addCell(text("end"));
		table.getHeader().addCell(text("count"));
		table.getHeader().addCell(text("failures"));
		table.getHeader().addCell(text("firstSuccess"));
		table.getHeader().addCell(text("lastSuccess"));
		table.getHeader().addCell(text("firstFailed"));
		table.getHeader().addCell(text("lastFailed"));
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
		row.addCell(text("total"));
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
	
	private List<DataPoint> getSuccessfulApdexData(List<ConcurrentRequestData> stats) {
		List<DataPoint> points = new ArrayList<>();
		for (ConcurrentRequestData data : stats) {
			points.add(point(data.count(), data.getSuccessfulApdex()));
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
			tools.error(text("cannotFitApdexFunction"), ex);
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
	
	private boolean hasNon1Apdex(List<ConcurrentRequestData> stats, boolean successful) {
		for (ConcurrentRequestData data : stats) {
			double apdex = successful ? data.getSuccessfulApdex() : data.getApdex();
			if (apdex < 1.0) {
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
			tools.error(text("cannotFitFailureFunction"), ex);
		}
		return function;
	}
	
	private Function getThroughputFunction(List<DataPoint> points) {
		if (points.size() < 3) {
			return null;
		}
		try {
			Function function = functions.getLogisticThroughputFunction(points);
			tools.regression().getNonLinearRegression(function, points).run();
			return function;
		} catch (Exception ex) {
			tools.error(text("cannotFitThroughputFunction"), ex);
			return null;
		}
	}

	private List<JmeterResult> loadResults(File file, String encoding, String keepPattern, String skipPattern) throws PluginException {
		List<JmeterResult> results;
		try (Reader reader = new InputStreamReader(new FileInputStream(file), encoding)) {
			results = tools.csv().fromCsv(reader, JmeterResult.class);
		} catch (IOException|TransformerException ex) {
			throw new PluginException(text("cannotLoadResultFile"), ex);
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
	
	private List<DataPoint> loadDataset(File file) throws PluginException {
		List<DataPointImpl> dataset;
		try (Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
			dataset = tools.csv().fromCsv(reader, DataPointImpl.class);
		} catch (IOException|TransformerException ex) {
			throw new PluginException(text("cannotLoadCsvFile"), ex);
		}
		return (List)dataset;
	}
	
	private String text(String key) {
		return resources.getString(key);
	}
	
	private Comparator<GroupedRequestData> getStatComparator() {
		return (stat1, stat2) -> {
			int i = stat1.getCount() - stat2.getCount();
			if (i == 0) {
				if (stat1.getLabel() == null) {
					i = -1;
				} else if (stat2.getLabel() == null) {
					i = 1;
				} else {
					i = stat1.getLabel().compareTo(stat2.getLabel());
				}
			}
			return i;
		};
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
