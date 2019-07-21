package cn.panshi.etf4j.robust;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * id是物理键
 * transTypeEnumClazz、transType、bizId三要素是逻辑主键——联合唯一索引
 */
@SuppressWarnings("serial")
public class EtfRobTxRecord implements Serializable {
	String transTypeEnumClazz;

	String transType;

	String bizId;

	// 以上3个字段逻辑主键——联合唯一索引。

	String bizStateJson;

	Boolean transSuccess;

	String transResultJson;// 交易成功时，记下返回值，用于重复调用时幂等校验 直接返回结果；

	Date crtDate;

	Date lastUpdateDate;

	Integer retryCount;

	Date nextRetryTime;

	Integer queryCount;

	Date nextQueryTime;

	Boolean queryTransSuccess;

	List<EtfRobTxRecordLog> logList = new ArrayList<>();

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

	public String getBizStateJson() {
		return bizStateJson;
	}

	public void setBizStateJson(String bizStateJson) {
		this.bizStateJson = bizStateJson;
	}

	public Boolean getTransSuccess() {
		return transSuccess;
	}

	public void setTransSuccess(Boolean transSuccess) {
		this.transSuccess = transSuccess;
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

	public Integer getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(Integer retryCount) {
		this.retryCount = retryCount;
	}

	public Date getNextRetryTime() {
		return nextRetryTime;
	}

	public void setNextRetryTime(Date nextRetryTime) {
		this.nextRetryTime = nextRetryTime;
	}

	public Integer getQueryCount() {
		return queryCount;
	}

	public void setQueryCount(Integer queryCount) {
		this.queryCount = queryCount;
	}

	public Date getNextQueryTime() {
		return nextQueryTime;
	}

	public void setNextQueryTime(Date nextQueryTime) {
		this.nextQueryTime = nextQueryTime;
	}

	public Boolean getQueryTransSuccess() {
		return queryTransSuccess;
	}

	public void setQueryTransSuccess(Boolean queryTransSuccess) {
		this.queryTransSuccess = queryTransSuccess;
	}

	public String getTransResultJson() {
		return transResultJson;
	}

	public void setTransResultJson(String transResultJson) {
		this.transResultJson = transResultJson;
	}

	public List<EtfRobTxRecordLog> getLogList() {
		return logList;
	}

	public void setLogList(List<EtfRobTxRecordLog> logList) {
		this.logList = logList;
	}

}
