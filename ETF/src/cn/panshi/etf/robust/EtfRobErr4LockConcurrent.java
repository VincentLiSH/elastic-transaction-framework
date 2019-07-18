package cn.panshi.etf.robust;

@SuppressWarnings("serial")
public class EtfRobErr4LockConcurrent extends Exception {
	String error;

	public EtfRobErr4LockConcurrent(String error) {
		super(error);
		this.error = error;
	}

	public String getError() {
		return error;
	}
}