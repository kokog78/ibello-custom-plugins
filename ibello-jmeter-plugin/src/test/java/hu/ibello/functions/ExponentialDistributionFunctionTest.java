package hu.ibello.functions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.Test;

public class ExponentialDistributionFunctionTest {

	@Test
	public void inverse_function_should_return_the_right_value() throws Exception {
		Function fn = function(5, 2);
		Function inverse = inverse(fn);
		double value1 = fn.value(1);
		assertThat(inverse.value(value1)).isEqualTo(1.0, within(0.0000001));
		double value2 = fn.value(5);
		assertThat(inverse.value(value2)).isEqualTo(5.0, within(0.0000001));
		double value3 = fn.value(10);
		assertThat(inverse.value(value3)).isEqualTo(10.0, within(0.0000001));
	}
	
	private Function function(double y0, double lambda) {
		ExponentialDistributionFunction fn = new ExponentialDistributionFunction();
		fn.setY0(y0);
		fn.setLambda(lambda);
		return fn;
	}
	
	private Function inverse(Function function) {
		ExponentialDistributionInverseFunction inverse = new ExponentialDistributionInverseFunction();
		inverse.setParameters(function.getParameters());
		return inverse;
	}
}
