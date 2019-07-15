package cn.panshi.etf.tcc;

import java.util.List;

import cn.panshi.etf.core.EtfAbstractRedisLockTemplate;

public class EtfTccDaoRedis implements EtfTccDao {

	@Override
	public boolean addEtfTccTransPrepareList(String name, String bizId, String tccEnumValue) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<String> findTccTransList2Start(String name, String tccTransBizId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void startTccTransByPreparedKey(String key) {
		// TODO Auto-generated method stub

	}

	@Override
	public EtfAbstractRedisLockTemplate getEtfTccConcurrentLock(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String popTccTransListOnTrySuccess() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void triggerTccConfirmOrCancel() {
		// TODO Auto-generated method stub

	}

	@Override
	public void popTccTransListAndFlagTccFailure() {
		// TODO Auto-generated method stub

	}

	@Override
	public String popTccCancelListOnCancelFinished() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateTccCanceled() {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateTccCancelFailure() {
		// TODO Auto-generated method stub

	}

	@Override
	public String popTccConfirmListOnSuccess() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateTccSuccess() {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateTccFailure() {
		// TODO Auto-generated method stub

	}

}
