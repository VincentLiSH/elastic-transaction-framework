package cn.panshi.etf.robust;

@SuppressWarnings("serial")
public class EtfRobErr4TransNeedRetry extends RuntimeException {
	String error;

	public EtfRobErr4TransNeedRetry(String error) {
		super(error);
		this.error = error;
	}

	public String getError() {
		return error;
	}
}