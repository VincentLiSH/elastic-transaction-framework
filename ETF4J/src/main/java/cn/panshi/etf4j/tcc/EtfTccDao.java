package cn.panshi.etf4j.tcc;

import cn.panshi.etf4j.robust.EtfAbstractRedisLockTemplate;

public interface EtfTccDao {
	final static int DEFAULT_SUCCESS_LIST_SIZE = 3000;
	final static int DEFAULT_CANCELED_LIST_SIZE = 3000;

	EtfTccStep loadTccTransRecordStep(String transTypeEnumClazz, String transType, String bizId);

	void startTccTransAsynch(String tccTransClass, String tccTransEnumValue, String tccTransBizId);

	EtfAbstractRedisLockTemplate getEtfTccConcurrentLock(int lockAutoExpireSeconds);

	String popTccTryCountorListOnSuccess(String tccTransBizId, String transTypeEnumClazz);

	void triggerTccConfirmOrCancel(String tccTransBizId, String transTypeEnumClazz);

	void popTccTransListAndFlagTccFailure(String tccTransBizId, String transTypeEnumClazz, String transTypeEnumValue);

	String popTccCancelCountorListOnFinished(String transTypeEnumClazz, String tccTransBizId);

	void pushTccCanceledList(String tccEnumClazzName, String tccTransBizId);

	void popTccCancelCountorAndFlagFailure(String tccEnumClazzName, String tccTransBizId, Exception e);

	String popTccConfirmCountorListOnSuccess(String tccEnumClazzName, String tccTransBizId);

	void pushTccSuccessList(String tccEnumClazzName, String tccTransBizId);

	void popTccConfirmCountorAndFlagFailure(String tccEnumClazzName, String tccTransBizId, Exception e);

	void saveNewEtfTccStep(String tccEnumClassName, String bizId, String tccEnumValue, String bizStateJson)
			throws EtfTccException4PrepareStage;

	void initTccCounter4Try(String tccEnumClassName, String bizId) throws EtfTccException4PrepareStage;

	void updateTccErrorDetail(String tccTransBizId, String transTypeEnumClazz, String transTypeEnumValue, Exception e);

}