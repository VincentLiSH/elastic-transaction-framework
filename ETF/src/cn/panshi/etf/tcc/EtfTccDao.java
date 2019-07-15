package cn.panshi.etf.tcc;

import java.util.Set;

import cn.panshi.etf.core.EtfAbstractRedisLockTemplate;

public interface EtfTccDao {
	EtfTccRecordStep loadTccTransRecordStep(String transTypeEnumClazz, String transType, String bizId);

	boolean addEtfTccTransPrepareList(String name, String bizId, String tccEnumValue)
			throws EtfTccException4PrepareStage;

	Set<String> findTccTransList2Start(String name, String tccTransBizId);

	void startTccTransByPreparedKey(String tccTransClass, String tccTransEnumValue, String tccTransBizId);

	EtfAbstractRedisLockTemplate getEtfTccConcurrentLock(int i);

	String popTccTransListOnTrySuccess();

	void triggerTccConfirmOrCancel();

	void popTccTransListAndFlagTccFailure();

	String popTccCancelListOnCancelFinished();

	void updateTccCanceled();

	void updateTccCancelFailure();

	String popTccConfirmListOnSuccess();

	void updateTccSuccess();

	void updateTccFailure();

	void saveEtfTccRecordStep(String tccEnumClassName, String bizId, String tccEnumValue, String bizStateJson);

}
