package cn.panshi.etf.tcc;

import cn.panshi.etf.robust.EtfAbstractRedisLockTemplate;

public interface EtfTccDao {
	EtfTccStep loadTccTransRecordStep(String transTypeEnumClazz, String transType, String bizId);

	void startTccTransAsynch(String tccTransClass, String tccTransEnumValue, String tccTransBizId);

	EtfAbstractRedisLockTemplate getEtfTccConcurrentLock(int lockAutoExpireSeconds);

	String popTccTryCountorListOnSuccess(String tccTransBizId, String transTypeEnumClazz);

	void triggerTccConfirmOrCancel(String tccTransBizId, String transTypeEnumClazz);

	void popTccTransListAndFlagTccFailure(String tccTransBizId, String transTypeEnumClazz, String transTypeEnumValue);

	String popTccCancelCountorListOnFinished(String transTypeEnumClazz, String tccTransBizId);

	void updateTccCanceled(String tccEnumClazzName, String tccTransBizId);

	void popTccCancelCountorAndFlagFailure(String tccEnumClazzName, String tccTransBizId, Exception e);

	String popTccConfirmCountorListOnSuccess(String tccEnumClazzName, String tccTransBizId);

	void updateTccSuccess(String tccEnumClazzName, String tccTransBizId);

	void popTccConfirmCountorAndFlagFailure(String tccEnumClazzName, String tccTransBizId, Exception e);

	void saveEtfTccStep(String tccEnumClassName, String bizId, String tccEnumValue, String bizStateJson);

	void initTccCounter4Try(String tccEnumClassName, String bizId) throws EtfTccException4PrepareStage;

}