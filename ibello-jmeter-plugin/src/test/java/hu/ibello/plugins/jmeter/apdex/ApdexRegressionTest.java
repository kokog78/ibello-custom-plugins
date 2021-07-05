package hu.ibello.plugins.jmeter.apdex;

import java.util.List;

import org.junit.Test;

import hu.ibello.plugins.jmeter.regression.DataPoint;

public class ApdexRegressionTest extends AbstractRegressionTest {

	@Test
	public void test() throws Exception {
		List<DataPoint> data = points(
				point(1, 1),
				point(5, 1),
				point(10, 1),
				point(20, 0.7),
				point(50, 0.36),
				point(100, 0.155),
				point(200, 0.085)
			);
		ApdexRegression apdex = new ApdexRegression(data);
		apdex.initialize();
		System.out.println(apdex.getFunction().toString());
		apdex.run();
		System.out.println(apdex.getFunction().toString());
	}
	
}
