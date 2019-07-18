package cn.panshi.etf.robust;

@SuppressWarnings("serial")
public class EtfRobErr4InvalidTransType extends Exception {
	String error;

	public EtfRobErr4InvalidTransType(String error) {
		super(error);
		this.error = error;
	}

	public String getError() {
		return error;
	}

}
