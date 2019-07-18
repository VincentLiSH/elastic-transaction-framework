package cn.panshi.etf.robust;

import java.util.Date;

public interface EtfRobDao {

	void validateTransDuplicate(EtfRobTxRecord tr) throws EtfRobErr4TransDuplicate;

	EtfRobTxRecord loadEtfTransRecord(String transTypeEnumClazz, String transType, String bizId);

	EtfAbstractRedisLockTemplate getEtfConcurrentLock(String transTypeEnumClazz, String transTypeCode, String bizId,
			int expireSeconds);

	String saveTransRecord(EtfRobTxRecord tr);

	void updateTransRecordNextRetry(EtfRobTxRecord tr, Date calcNextRetryTime);

	void updateTransRecordRetrySuccess(EtfRobTxRecord tr, String resultJson);

	void updateTransMaxRetryTimesAndInsertFailureList(EtfRobTxRecord tr);

	void insertEtfRetryQueueAndTimer(EtfRobTxRecord tr);

	void addTrTransLog(EtfRobTxRecord tr, EtfRobTxRecordLog etfLog);

	void insertEtfQueryQueueAndTimer(EtfRobTxRecord tr);

	void updateTransMaxQueryTimesAndInsertFailureList(EtfRobTxRecord tr);

	void updateTransRecordNextQuery(EtfRobTxRecord tr, Date nextQueryTime);

	void updateTransRecordQuerySuccess(EtfRobTxRecord tr);

	void addTransDuplicateInvokeLog(EtfRobTxRecord tr);

	void updateTransRecordQueryFailure(EtfRobTxRecord tr);

	void deleteEtfRetryQueueByTimerKey(String currEtfTransRetryTimerKey);

	void deleteEtfQueryQueueByTimerKey(String currEtfTransQueryTimerKey);
}