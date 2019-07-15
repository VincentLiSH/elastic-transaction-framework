package cn.panshi.etf.tcc;

import java.util.List;

import cn.panshi.etf.core.EtfAbstractRedisLockTemplate;

public interface EtfTccDao {

	boolean addEtfTccTransPrepareList(String name, String bizId, String tccEnumValue);

	List<String> findTccTransList2Start(String name, String tccTransBizId);

	void startTccTransByPreparedKey(String key);

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

}
