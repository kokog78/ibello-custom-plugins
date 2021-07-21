package hu.ibello.functions;

public class CumulativeRayleighInverseFunction extends CumulativeRayleighFunction {

	@Override
	public double value(double x) {
		return x0 + sigma * Math.sqrt(-2 * Math.log(1 - x));
	}
	
	@Override
	public String toString() {
		String _x0 = getFormattedParameter(0);
		String _sigma = getFormattedParameter(1);
		return String.format("%s + %s * (-2 * ln(1 - x)) ^(1/2)", _x0, _sigma);
	}
}
