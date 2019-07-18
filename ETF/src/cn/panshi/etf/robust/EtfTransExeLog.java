package cn.panshi.etf.robust;

import java.io.Serializable;
import java.util.Date;

@SuppressWarnings("serial")
public class EtfTransExeLog implements Serializable {
	public enum TRANS_EXE_MODE {
		normal, retry, after_success, duplicate;
	}

	TRANS_EXE_MODE logType;

	Date crtDate;

	String log;

	String error;

	public TRANS_EXE_MODE getLogType() {
		return logType;
	}

	public void setLogType(TRANS_EXE_MODE logType) {
		this.logType = logType;
	}

	public Date getCrtDate() {
		return crtDate;
	}

	public void setCrtDate(Date crtDate) {
		this.crtDate = crtDate;
	}

	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}
}