package cn.panshi.etf.core;

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