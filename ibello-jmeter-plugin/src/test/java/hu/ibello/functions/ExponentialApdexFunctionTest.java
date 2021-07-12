package hu.ibello.functions;

import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

public class ExponentialApdexFunctionTest {

	@Test
	public void inverse_function_should_return_the_right_value() throws Exception {
		Function fn = function(1, 2);
		Function inverse = inverse(fn);
		double value1 = fn.value(1);
		assertThat(inverse.value(value1)).isEqualTo(1.0, within(0.0000001));
		double value2 = fn.value(5);
		assertThat(inverse.value(value2)).isEqualTo(5.0, within(0.0000001));
		double value3 = fn.value(10);
		assertThat(inverse.value(value3)).isEqualTo(10.0, within(0.0000001));
	}
	
	private Function function(double x0, double c) {
		ExponentialApdexFunction fn = new ExponentialApdexFunction();
		fn.setX0(x0);
		fn.setC(c);
		return fn;
	}
	
	private Function inverse(Function function) {
		ExponentialApdexInverseFunction inverse = new ExponentialApdexInverseFunction();
		inverse.setParameters(function.getParameters());
		return inverse;
	}
}
