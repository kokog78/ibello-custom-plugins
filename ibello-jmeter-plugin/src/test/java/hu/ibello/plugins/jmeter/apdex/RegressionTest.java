package hu.ibello.plugins.jmeter.apdex;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.Test;

import hu.ibello.plugins.jmeter.functions.LinearFunction;
import hu.ibello.plugins.jmeter.regression.Regression;
import hu.ibello.regression.AbstractRegressionTest;
import hu.ibello.regression.DataPoint;

public class RegressionTest extends AbstractRegressionTest {

	@Test
	public void test_perfect_fit() throws Exception {
		List<DataPoint> data = points(
				point(1, 2),
				point(2, 4),
				point(3, 6)
			);
		LinearFunction linear = new LinearFunction(1, 1);
		Regression<LinearFunction> regression = new Regression<LinearFunction>(linear, data);
		regression.run();
		System.out.println(linear.toString());
		assertThat(linear.getA()).isEqualTo(0.0, within(0.001));
		assertThat(linear.getB()).isEqualTo(2.0, within(0.001));
	}

	@Test
	public void test_fit() throws Exception {
		List<DataPoint> data = points(
				point(1, 1.9),
				point(2, 4.1),
				point(3, 6.1)
			);
		LinearFunction linear = new LinearFunction(1, 1);
		Regression<LinearFunction> regression = new Regression<LinearFunction>(linear, data);
		regression.run();
		System.out.println(linear.toString());
		assertThat(linear.getA()).isEqualTo(-0.1, within(0.1));
		assertThat(linear.getB()).isEqualTo(2.0, within(0.1));
	}
}
