package cn.panshi.etf.tcc;

import java.util.Set;

import cn.panshi.etf.core.EtfAbstractRedisLockTemplate;

public interface EtfTccDao {
	EtfTccStep loadTccTransRecordStep(String transTypeEnumClazz, String transType, String bizId);

	boolean addEtfTccTransPrepareList(String name, String bizId, String tccEnumValue)
			throws EtfTccException4PrepareStage;

	Set<String> findTccTransList2Start(String name, String tccTransBizId);

	void startTccTransAsynch(String tccTransClass, String tccTransEnumValue, String tccTransBizId);

	EtfAbstractRedisLockTemplate getEtfTccConcurrentLock(int lockAutoExpireSeconds);

	String popTccTransListOnTrySuccess(String tccTransBizId, String transTypeEnumClazz);

	void triggerTccConfirmOrCancel(String tccTransBizId, String transTypeEnumClazz);

	void popTccTransListAndFlagTccFailure(String tccTransBizId, String transTypeEnumClazz, String transTypeEnumValue);

	String popTccCancelListOnCancelFinished();

	void updateTccCanceled();

	void updateTccCancelFailure();

	String popTccConfirmListOnSuccess();

	void updateTccSuccess();

	void updateTccFailure();

	void saveEtfTccRecordStep(String tccEnumClassName, String bizId, String tccEnumValue, String bizStateJson);

}