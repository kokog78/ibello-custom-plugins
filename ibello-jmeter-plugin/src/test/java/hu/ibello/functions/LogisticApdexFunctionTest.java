package hu.ibello.functions;

import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

public class LogisticApdexFunctionTest {

	@Test
	public void inverse_function_should_return_the_right_value() throws Exception {
		InversableFunction fn = function(1, 2, 3, 4);
		Function inverse = inverse(fn);
		double value1 = fn.value(1);
		assertThat(inverse.value(value1)).isEqualTo(1.0, within(0.0000001));
		double value2 = fn.value(5);
		assertThat(inverse.value(value2)).isEqualTo(5.0, within(0.0000001));
		double value3 = fn.value(10);
		assertThat(inverse.value(value3)).isEqualTo(10.0, within(0.0000001));
	}
	
	private InversableFunction function(double y0, double y1, double b, double c) {
		LogisticApdexFunction fn = new LogisticApdexFunction();
		fn.setY0(y0);
		fn.setY1(y1);
		fn.setB(b);
		fn.setC(c);
		return fn;
	}
	
	private Function inverse(InversableFunction function) {
		return function.getInverseFunction();
	}
}
