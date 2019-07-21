package cn.panshi.etf4j.robust;

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