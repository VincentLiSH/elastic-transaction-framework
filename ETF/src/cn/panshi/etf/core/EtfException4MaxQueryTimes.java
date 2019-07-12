package cn.panshi.etf.core;

@SuppressWarnings("serial")
public class EtfException4MaxQueryTimes extends RuntimeException {
	String error;

	public EtfException4MaxQueryTimes(String error) {
		super();
		this.error = error;
	}

	public String getError() {
		return error;
	}
}