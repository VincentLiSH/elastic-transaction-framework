package cn.panshi.etf.robust;

@SuppressWarnings("serial")
public class EtfException4MaxQueryTimes extends RuntimeException {
	String error;

	public EtfException4MaxQueryTimes(String error) {
		super(error);
		this.error = error;
	}

	public String getError() {
		return error;
	}
}