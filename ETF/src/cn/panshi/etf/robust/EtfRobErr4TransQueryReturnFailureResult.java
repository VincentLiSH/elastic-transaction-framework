package cn.panshi.etf.robust;

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
