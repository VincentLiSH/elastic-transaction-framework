package cn.panshi.etf4j.tcc;

@SuppressWarnings("serial")
public class EtfTccException4PrepareStage extends Exception {
	String error;

	public EtfTccException4PrepareStage(String error) {
		super(error);
		this.error = error;
	}

	public String getError() {
		return error;
	}
}