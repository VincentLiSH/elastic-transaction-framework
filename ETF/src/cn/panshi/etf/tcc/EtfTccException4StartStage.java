package cn.panshi.etf.tcc;

@SuppressWarnings("serial")
public class EtfTccException4StartStage extends Exception {
	String error;

	public EtfTccException4StartStage(String error) {
		super();
		this.error = error;
	}

	public String getError() {
		return error;
	}
}
