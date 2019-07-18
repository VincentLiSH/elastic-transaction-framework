package cn.panshi.etf.robust;

@SuppressWarnings("serial")
public class EtfException4TransDuplicate extends Exception {
	EtfTransRecord etfTransRecord;

	public EtfException4TransDuplicate(EtfTransRecord tr) {
		super();
		this.etfTransRecord = tr;
	}

	public EtfTransRecord getEtfTransRecord() {
		return etfTransRecord;
	}
}