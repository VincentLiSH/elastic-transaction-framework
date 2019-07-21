package cn.panshi.etf4j.tcc;

@SuppressWarnings("serial")
public class EtfTccException4ReturnBizCode extends RuntimeException {
	String bizId;

	public EtfTccException4ReturnBizCode(String bizId) {
		super(bizId);
		this.bizId = bizId;
	}

	public String getBizId() {
		return bizId;
	}
}