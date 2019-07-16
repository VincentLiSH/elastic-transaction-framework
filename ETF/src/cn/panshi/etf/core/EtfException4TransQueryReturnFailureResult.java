package cn.panshi.etf.core;

@SuppressWarnings("serial")
public class EtfException4TransQueryReturnFailureResult extends Exception {
	String error;

	public EtfException4TransQueryReturnFailureResult(String error) {
		super(error);
		this.error = error;
	}

	public String getError() {
		return error;
	}
}
