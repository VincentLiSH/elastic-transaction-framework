package cn.panshi.etf.tcc;

import cn.panshi.etf.core.EtfAbstractRedisLockTemplate;

public interface EtfTccDao {
	EtfTccStep loadTccTransRecordStep(String transTypeEnumClazz, String transType, String bizId);

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

	void saveEtfTccStep(String tccEnumClassName, String bizId, String tccEnumValue, String bizStateJson);

	void initTccCounter4Try(String tccEnumClassName, String bizId) throws EtfTccException4PrepareStage;

}