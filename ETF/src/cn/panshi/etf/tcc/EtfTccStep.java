package cn.panshi.etf.tcc;

import java.util.Date;

public class EtfTccStep {
	String tccEnumValue;

	String bizStateJson;

	String error;

	Date crtDate;

	public String getTccEnumValue() {
		return tccEnumValue;
	}

	public void setTccEnumValue(String tccEnumValue) {
		this.tccEnumValue = tccEnumValue;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public Date getCrtDate() {
		return crtDate;
	}

	public void setCrtDate(Date crtDate) {
		this.crtDate = crtDate;
	}

	public String getBizStateJson() {
		return bizStateJson;
	}

	public void setBizStateJson(String bizStateJson) {
		this.bizStateJson = bizStateJson;
	}
}
