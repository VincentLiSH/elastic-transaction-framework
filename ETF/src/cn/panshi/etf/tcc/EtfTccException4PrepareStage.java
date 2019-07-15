package cn.panshi.etf.tcc;

@SuppressWarnings("serial")
public class EtfTccException4PrepareStage extends Exception {
	String error;

	public EtfTccException4PrepareStage(String error) {
		super();
		this.error = error;
	}

	public String getError() {
		return error;
	}
}
