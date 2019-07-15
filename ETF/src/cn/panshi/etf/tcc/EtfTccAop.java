package cn.panshi.etf.tcc;

public class EtfTccAop {
	private final static ThreadLocal<String> TCC_TRANS_PREPARE_STAGE = new ThreadLocal<>();

	public static void setCurrTccPrepareStage() {
		TCC_TRANS_PREPARE_STAGE.set("");
	}

	public static String getCurrBizId() {
		// TODO Auto-generated method stub
		return null;
	}

}
