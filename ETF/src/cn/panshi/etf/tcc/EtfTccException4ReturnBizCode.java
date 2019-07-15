package cn.panshi.etf.tcc;

<<<<<<< HEAD
@SuppressWarnings("serial")
=======
>>>>>>> branch 'dev' of https://github.com/VincentLiSH/elastic-transaction-framework.git
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
