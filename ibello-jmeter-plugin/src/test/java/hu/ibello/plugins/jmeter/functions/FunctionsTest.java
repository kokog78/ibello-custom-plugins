package hu.ibello.plugins.jmeter.functions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import hu.ibello.regression.functions.Functions;

public class FunctionsTest {

	@Test
	public void exp_should_handle_infinite_values() throws Exception {
		assertThat(Functions.exp(Double.NEGATIVE_INFINITY)).isZero();
		assertThat(Functions.exp(Double.POSITIVE_INFINITY)).isInfinite().isPositive();
	}
	
	@Test
	public void exp_should_handle_zero_value() throws Exception {
		assertThat(Functions.exp(0.0)).isOne();
	}
	
	@Test
	public void exp_should_handle_negative_value() throws Exception {
		assertThat(Functions.exp(-1.0)).isLessThan(1.0);
	}
	
	@Test
	public void exp_should_handle_positive_value() throws Exception {
		assertThat(Functions.exp(1.0)).isGreaterThan(1.0);
	}
	
	@Test
	public void ln_should_handle_infinite_values() throws Exception {
		assertThat(Functions.ln(Double.NEGATIVE_INFINITY)).isNaN();
		assertThat(Functions.ln(Double.POSITIVE_INFINITY)).isInfinite().isPositive();
	}
	
	@Test
	public void ln_should_handle_zero_value() throws Exception {
		assertThat(Functions.ln(0.0)).isInfinite().isNegative();
	}
	
	@Test
	public void ln_should_handle_negative_value() throws Exception {
		assertThat(Functions.ln(-1.0)).isNaN();
	}
	
	@Test
	public void ln_should_handle_1_value() throws Exception {
		assertThat(Functions.ln(1.0)).isZero();
	}
	
	@Test
	public void ln_should_handle_small_value() throws Exception {
		assertThat(Functions.ln(0.5)).isNegative();
	}
	
	@Test
	public void ln_should_handle_big_value() throws Exception {
		assertThat(Functions.ln(10)).isPositive();
	}
	
	@Test
	public void pow_should_handle_infinite_powers() throws Exception {
		assertThat(Functions.pow(-1, Double.POSITIVE_INFINITY)).isNaN();
		assertThat(Functions.pow(-1, Double.NEGATIVE_INFINITY)).isNaN();
		assertThat(Functions.pow(0, Double.POSITIVE_INFINITY)).isZero();
		assertThat(Functions.pow(0, Double.NEGATIVE_INFINITY)).isZero();
		assertThat(Functions.pow(0.5, Double.POSITIVE_INFINITY)).isZero();
		assertThat(Functions.pow(0.5, Double.NEGATIVE_INFINITY)).isInfinite().isPositive();
		assertThat(Functions.pow(1, Double.POSITIVE_INFINITY)).isOne();
		assertThat(Functions.pow(1, Double.NEGATIVE_INFINITY)).isOne();
		assertThat(Functions.pow(1.5, Double.POSITIVE_INFINITY)).isInfinite().isPositive();
		assertThat(Functions.pow(1.5, Double.NEGATIVE_INFINITY)).isZero();
	}
	
	@Test
	public void pow_should_handle_zero_power() throws Exception {
		assertThat(Functions.pow(-1, 0)).isOne();
		assertThat(Functions.pow(0, 0)).isZero();
		assertThat(Functions.pow(0.5, 0)).isOne();
		assertThat(Functions.pow(1, 0)).isOne();
		assertThat(Functions.pow(1.5, 0)).isOne();
	}
	
	@Test
	public void pow_should_handle_one_power() throws Exception {
		assertThat(Functions.pow(-1, 1)).isEqualTo(-1.0);
		assertThat(Functions.pow(0, 1)).isZero();
		assertThat(Functions.pow(0.5, 1)).isEqualTo(0.5);
		assertThat(Functions.pow(1, 1)).isOne();
		assertThat(Functions.pow(1.5, 1)).isEqualTo(1.5);
	}
	
	@Test
	public void reciprocal_should_handle_infinite_values() throws Exception {
		assertThat(Functions.reciprocal(Double.NEGATIVE_INFINITY)).isZero();
		assertThat(Functions.reciprocal(Double.POSITIVE_INFINITY)).isZero();
	}

	@Test
	public void reciprocal_should_handle_zero_value() throws Exception {
		assertThat(Functions.reciprocal(0)).isInfinite().isPositive();
	}

	@Test
	public void reciprocal_should_handle_positive_values() throws Exception {
		assertThat(Functions.reciprocal(0.5)).isPositive().isGreaterThan(1.0);
		assertThat(Functions.reciprocal(1)).isOne();
		assertThat(Functions.reciprocal(1.5)).isPositive().isLessThan(1.0);
	}
}
