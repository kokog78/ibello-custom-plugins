package hu.ibello.plugins.jmeter.functions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class FiveParameterLogisticFunctionTest {

	@Test
	public void getValue_should_handle_zero_values() throws Exception {
		assertThat(getValue(0, 1, 2, 3, 4, 1)).isBetween(2.0, 3.0);
		assertThat(getValue(1, 0, 2, 3, 4, 1)).isEqualTo(2.875);
		assertThat(getValue(1, 2, 0, 3, 4, 1)).isEqualTo(3.0);
		assertThat(getValue(1, 2, 3, 0, 4, 1)).isBetween(0.0, 1.0);
		assertThat(getValue(1, 2, 3, 4, 0, 1)).isEqualTo(1.0);
		assertThat(getValue(1, 2, 3, 4, 5, 0)).isEqualTo(1.0);
	}
	
	@Test
	public void getValue_should_handle_infinite_results() throws Exception {
		assertThat(getValue(0, 1, 1, 1, 1, -1)).isInfinite().isNegative();
		assertThat(getValue(1, 1, 1, 0, 1, -1)).isInfinite().isPositive();
	}
	
	private double getValue(double a, double b, double c, double d, double m, double x) {
		double result = new FiveParameterLogisticFunction(a, b, c, d, m).getValue(x);
		System.out.println(result);
		return result;
	}
}
