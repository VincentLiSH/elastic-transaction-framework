package cn.panshi.etf4j.robust;

@SuppressWarnings("serial")
public class EtfRobErr4MaxQueryTimes extends RuntimeException {
	String error;

	public EtfRobErr4MaxQueryTimes(String error) {
		super(error);
		this.error = error;
	}

	public String getError() {
		return error;
	}
}