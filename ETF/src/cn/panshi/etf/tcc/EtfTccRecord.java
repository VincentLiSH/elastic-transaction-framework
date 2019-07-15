package cn.panshi.etf.tcc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EtfTccRecord {
	public enum TCC_TRANS_RESULT {
		success, canceled, confirm_failure, cancel_failure;
	}

	String transTypeEnumClazz;

	String transType;

	String bizId;

	// 以上3个字段逻辑主键——联合唯一索引。

	TCC_TRANS_RESULT transResult;

	Date crtDate;

	Date lastUpdateDate;

	List<EtfTccRecordStep> stepList = new ArrayList<>();

	public String getTransTypeEnumClazz() {
		return transTypeEnumClazz;
	}

	public void setTransTypeEnumClazz(String transTypeEnumClazz) {
		this.transTypeEnumClazz = transTypeEnumClazz;
	}

	public String getTransType() {
		return transType;
	}

	public void setTransType(String transType) {
		this.transType = transType;
	}

	public String getBizId() {
		return bizId;
	}

	public void setBizId(String bizId) {
		this.bizId = bizId;
	}

	public TCC_TRANS_RESULT getTransResult() {
		return transResult;
	}

	public void setTransResult(TCC_TRANS_RESULT transResult) {
		this.transResult = transResult;
	}

	public Date getCrtDate() {
		return crtDate;
	}

	public void setCrtDate(Date crtDate) {
		this.crtDate = crtDate;
	}

	public Date getLastUpdateDate() {
		return lastUpdateDate;
	}

	public void setLastUpdateDate(Date lastUpdateDate) {
		this.lastUpdateDate = lastUpdateDate;
	}

	public List<EtfTccRecordStep> getStepList() {
		return stepList;
	}

	public void setStepList(List<EtfTccRecordStep> stepList) {
		this.stepList = stepList;
	}

}