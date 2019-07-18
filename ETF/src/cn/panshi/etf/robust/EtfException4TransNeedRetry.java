package cn.panshi.etf.robust;

@SuppressWarnings("serial")
public class EtfException4TransNeedRetry extends RuntimeException {
	String error;

	public EtfException4TransNeedRetry(String error) {
		super(error);
		this.error = error;
	}

	public String getError() {
		return error;
	}
}