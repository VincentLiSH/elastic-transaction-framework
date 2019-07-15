package cn.panshi.etf.tcc;

@SuppressWarnings("serial")
public class EtfTccException4ReturnBizCode extends RuntimeException {
	String bizId;

	public EtfTccException4ReturnBizCode(String bizId) {
		super();
		this.bizId = bizId;
	}

	public String getBizId() {
		return bizId;
	}
}
