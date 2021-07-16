package hu.ibello.functions;

public class LogisticErrorFunction extends Logistic5Function implements X0Function {

	@Override
	public double getX0() {
		return getC();
	}
}
