package cn.panshi.etf.tcc;

import java.lang.reflect.ParameterizedType;

import org.apache.log4j.Logger;

import cn.panshi.etf.core.EtfException4LockConcurrent;

@SuppressWarnings("unchecked")
public abstract class EtfTccTransTemplate<T_tcc_trans_enum extends Enum<T_tcc_trans_enum>> {
	static Logger logger = Logger.getLogger(EtfTccTransTemplate.class);

	Class<T_tcc_trans_enum> tccTransEnumClass = (Class<T_tcc_trans_enum>) ((ParameterizedType) getClass()
			.getGenericSuperclass()).getActualTypeArguments()[0];

	EtfTccDao etfTccDao;

	private T_tcc_trans_enum tccTransEnum;

	protected EtfTccTransTemplate(EtfTccDao etfTccDao) {
		super();
		this.etfTccDao = etfTccDao;
	}

	public enum TCC_TRANS_STAGE {
		tcc_prepare, tcc_try, tcc_confirm, tcc_cancel;
	}

	public final void executeWithinEtfTcc() throws EtfException4LockConcurrent {
		TCC_TRANS_STAGE stage = calcCurrTccStage();

		if (stage == TCC_TRANS_STAGE.tcc_prepare) {
			this.exeTccPrepare();
		} else {
			String bizId = EtfTccAop.getCurrBizId();
			//			EtfAbstractRedisLockTemplate etfLock = etfTccDao.getEtfTccConcurrentLock(600);

			boolean lockSuccess = true;// etfLock.lock();

			try {
				if (!lockSuccess) {
					String error = "TCC交易" + getCurrEtfTransExeKey(tccTransEnum, bizId) + "获取锁失败";
					logger.warn(error);
					throw new EtfException4LockConcurrent(error);
				}

				if (stage == TCC_TRANS_STAGE.tcc_try) {
					this.exeTccTry();
				} else if (stage == TCC_TRANS_STAGE.tcc_confirm) {
					this.exeTccConfirm();
				} else if (stage == TCC_TRANS_STAGE.tcc_cancel) {
					this.exeTccCancel();
				}
			} finally {
				if (lockSuccess) {
					//					Long unlock = etfLock.unlock();
					//					logger.debug(
					//							"TCC交易" + getCurrEtfTransExeKey(tccTransEnum, bizId) + "执行阶段" + stage + "后 释放锁：" + unlock);
				}
			}
		}
	}

	@SuppressWarnings("finally")
	private void exeTccPrepare() {
		String bizId = null;
		try {
			bizId = calcTccTransBizId();
		} finally {
			throw new EtfTccException4ReturnBizCode(bizId);//此exception用于返回bizId给TccTransStarter
		}
	}

	private void exeTccCancel() {
		try {
			tccCancel();
			String key = etfTccDao.popTccCancelListOnCancelFinished();
			if (key == null) {
				logger.debug("popTccCancelListOnCancelFinished返回null，表明当前所有TCC交易都已经cancel完成，可以标记整个交易canceled");
				etfTccDao.updateTccCanceled();
			}
		} catch (Exception e) {
			etfTccDao.updateTccCancelFailure();
		}
	}

	private void exeTccConfirm() {
		try {
			tccConfirm();
			String key = etfTccDao.popTccConfirmListOnSuccess();
			if (key == null) {
				logger.debug("popTccConfirmListOnSuccess返回null，表明当前所有TCC交易都已经confirm完成，可以标记整个交易success");
				etfTccDao.updateTccSuccess();
			}
		} catch (Exception e) {
			etfTccDao.updateTccFailure();
		}
	}

	private void exeTccTry() {
		String tccTransBizId = EtfTccAop.getCurrBizId();
		String transTypeEnumClazz = EtfTccAop.getCurrTccTransEnumClazzName();
		String transTypeEnumValue = EtfTccAop.getCURR_INVOKE_TCC_ENUM_VALUE();
		try {
			tccTry();

			String key = etfTccDao.popTccTransListOnTrySuccess(tccTransBizId, transTypeEnumClazz);
			if (key == null) {
				logger.debug("popTccTransListOnTrySuccess返回null，表明当前所有TCC交易都已经try完成，可以触发confirm");
				etfTccDao.triggerTccConfirmOrCancel(tccTransBizId, transTypeEnumClazz);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			etfTccDao.popTccTransListAndFlagTccFailure(tccTransBizId, transTypeEnumClazz, transTypeEnumValue);
		} finally {

		}
	}

	private TCC_TRANS_STAGE calcCurrTccStage() {
		return EtfTccAop.getCurrTccStage();
	}

	private String getCurrEtfTransExeKey(T_tcc_trans_enum type, String bizId) {
		return type.getClass().getName() + "." + type.toString() + "#" + bizId;
	}

	protected abstract String calcTccTransBizId();

	/**
	 * 
	 */
	protected abstract void tccTry();

	protected abstract void tccConfirm();

	protected abstract void tccCancel();

}