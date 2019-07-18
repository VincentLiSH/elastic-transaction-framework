package cn.panshi.etf.robust;

@SuppressWarnings("serial")
public class EtfRobErr4TransDuplicate extends Exception {
	EtfRobTxRecord etfRobTxRecord;

	public EtfRobErr4TransDuplicate(EtfRobTxRecord tr) {
		super();
		this.etfRobTxRecord = tr;
	}

	public EtfRobTxRecord getEtfTransRecord() {
		return etfRobTxRecord;
	}
}