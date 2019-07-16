package cn.panshi.etf.core;

@SuppressWarnings("serial")
public class EtfException4InvalidTransType extends Exception {
	String error;

	public EtfException4InvalidTransType(String error) {
		super(error);
		this.error = error;
	}

	public String getError() {
		return error;
	}

}
