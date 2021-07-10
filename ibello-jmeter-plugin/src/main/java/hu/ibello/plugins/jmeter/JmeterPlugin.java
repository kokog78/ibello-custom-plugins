package hu.ibello.plugins.jmeter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hu.ibello.plugins.IbelloTaskRunner;
import hu.ibello.plugins.PluginException;
import hu.ibello.plugins.PluginInitializer;
import hu.ibello.plugins.jmeter.model.ApdexData;
import hu.ibello.plugins.jmeter.model.JmeterResult;
import hu.ibello.transform.TransformerException;

public class JmeterPlugin implements IbelloTaskRunner {

	private final static String TASK_APDEX = "jmeter.apdex";
	private final static String PARAMETER_RESULT_FILE = "jmeter.file.result";
	private final static String PARAMETER_ENCODING = "jmeter.file.encoding";
	private final static String PARAMETER_THRESHOLD_SATISFIED = "jmeter.threshold.satisfied";
	private final static String PARAMETER_THRESHOLD_TOLERATED = "jmeter.threshold.tolerated";
	
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
				List<JmeterResult> results;
				try (Reader reader = new InputStreamReader(new FileInputStream(file), encoding)) {
					results = tools.csv().fromCsv(reader, JmeterResult.class);
				} catch (IOException|TransformerException ex) {
					throw new PluginException("Cannot load Jmeter result file", ex);
				}
				List<String> labels = new ArrayList<>();
				int labelSize = 0;
				ApdexData totalApdex = new ApdexData();
				Map<String, ApdexData> apdexMap = new HashMap<>();
				for (JmeterResult result : results) {
					if (!labels.contains(result.getLabel())) {
						labels.add(result.getLabel());
						if (result.getLabel().length() > labelSize) {
							labelSize = result.getLabel().length();
						}
					}
					ApdexData apdex = apdexMap.get(result.getLabel());
					if (apdex == null) {
						apdex = new ApdexData();
						apdexMap.put(result.getLabel(), apdex);
					}
					if (result.getElapsed() <= satisfactionThreshold) {
						apdex.satisfied();
						totalApdex.satisfied();
					} else if (result.getElapsed() <= tolerationThreshold) {
						apdex.tolerated();
						totalApdex.tolerated();
					} else {
						apdex.frustrated();
						totalApdex.frustrated();
					}
				}
				labelSize = Math.min(labelSize, 5);
				tools.info("APDEX of the selected results");
				String format = "- %" + labelSize + "s: %.3f";
				for (String label : labels) {
					ApdexData apdex = apdexMap.get(label);
					tools.info(String.format(format, label, apdex.getApdex()));
				}
				tools.info(String.format(format, "Total", totalApdex.getApdex()));
			}
		}
		return false;
	}

	@Override
	public void shutdown() throws PluginException {
	}

}
