package cn.panshi.etf4j.robust;

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