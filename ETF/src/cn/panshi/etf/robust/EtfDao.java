package cn.panshi.etf.robust;

import java.util.Date;

public interface EtfDao {

	void validateTransDuplicate(EtfTransRecord tr) throws EtfException4TransDuplicate;

	EtfTransRecord loadEtfTransRecord(String transTypeEnumClazz, String transType, String bizId);

	EtfAbstractRedisLockTemplate getEtfConcurrentLock(String transTypeEnumClazz, String transTypeCode, String bizId,
			int expireSeconds);

	String saveTransRecord(EtfTransRecord tr);

	void updateTransRecordNextRetry(EtfTransRecord tr, Date calcNextRetryTime);

	void updateTransRecordRetrySuccess(EtfTransRecord tr, String resultJson);

	void updateTransMaxRetryTimesAndInsertFailureList(EtfTransRecord tr);

	void insertEtfRetryQueueAndTimer(EtfTransRecord tr);

	void addTrTransLog(EtfTransRecord tr, EtfTransExeLog etfLog);

	void insertEtfQueryQueueAndTimer(EtfTransRecord tr);

	void updateTransMaxQueryTimesAndInsertFailureList(EtfTransRecord tr);

	void updateTransRecordNextQuery(EtfTransRecord tr, Date nextQueryTime);

	void updateTransRecordQuerySuccess(EtfTransRecord tr);

	void addTransDuplicateInvokeLog(EtfTransRecord tr);

	void updateTransRecordQueryFailure(EtfTransRecord tr);

	void deleteEtfRetryQueueByTimerKey(String currEtfTransRetryTimerKey);

	void deleteEtfQueryQueueByTimerKey(String currEtfTransQueryTimerKey);
}