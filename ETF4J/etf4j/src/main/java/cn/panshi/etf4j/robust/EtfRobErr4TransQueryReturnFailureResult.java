package cn.panshi.etf4j.robust;

@SuppressWarnings("serial")
public class EtfRobErr4TransQueryReturnFailureResult extends Exception {
	String error;

	public EtfRobErr4TransQueryReturnFailureResult(String error) {
		super(error);
		this.error = error;
	}

	public String getError() {
		return error;
	}
}
