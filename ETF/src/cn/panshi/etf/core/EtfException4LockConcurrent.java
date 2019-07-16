package cn.panshi.etf.core;

@SuppressWarnings("serial")
public class EtfException4LockConcurrent extends Exception {
	String error;

	public EtfException4LockConcurrent(String error) {
		super(error);
		this.error = error;
	}

	public String getError() {
		return error;
	}
}