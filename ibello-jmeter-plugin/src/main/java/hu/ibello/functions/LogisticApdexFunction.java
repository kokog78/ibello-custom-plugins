package hu.ibello.functions;

public class LogisticApdexFunction extends Logistic5Function {

	public LogisticApdexFunction() {
		this.y0 = 0.0;
		this.y1 = 1.0;
	}
	
	@Override
	public void setY0(double d) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setY1(double d) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int getParameterCount() {
		return 3;
	}
	
	@Override
	public double getParameter(int paramIndex) {
		return super.getParameter(paramIndex+2);
	}
	
	@Override
	public void setParameter(int paramIndex, double value) {
		super.setParameter(paramIndex+2, value);
	}
	
	@Override
	public String getFormattedParameter(int paramIndex) {
		return super.getFormattedParameter(paramIndex+2);
	}
	
	@Override
	public double[] getParameters() {
		return new double[] {getB(), getC(), getM()};
	}
	
	@Override
	public void setParameters(double... parameters) {
		setB(parameters[0]);
		setC(parameters[1]);
		setM(parameters[2]);
	}
	
	@Override
	public String toString() {
		String _b = getFormattedParameter(0);
		String _c = getFormattedParameter(1);
		String _m = getFormattedParameter(2);
		return String.format("1 / ((1 + (x / %s) ^%s) ^%s)", _c, _b, _m);
	}
	
}
