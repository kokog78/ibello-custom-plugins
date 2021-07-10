package hu.ibello.regression;

import java.util.List;

import org.junit.Test;

public class ApdexRegressionTest extends AbstractRegressionTest {

	@Test
	public void test() throws Exception {
		List<DataPoint> data = points(
				point(1, 1),
				point(5, 1),
				point(10, 0.9),
				point(20, 0.7),
				point(50, 0.35),
				point(100, 0.13),
				point(200, 0.058)
			);
		ApdexRegression apdex = new ApdexRegression(data);
		System.out.println(apdex.getFunction().toString());
		apdex.run();
		System.out.println(apdex.getFunction().toString());
	}
}
