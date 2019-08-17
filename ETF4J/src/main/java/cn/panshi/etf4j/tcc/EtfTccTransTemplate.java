package cn.panshi.etf4j.tcc;

import java.lang.reflect.ParameterizedType;

import org.apache.log4j.Logger;

import cn.panshi.etf4j.redis.AbstractRedisLockTemplate;
import cn.panshi.etf4j.robust.EtfRobErr4LockConcurrent;

@SuppressWarnings("unchecked")
public abstract class EtfTccTransTemplate<T_tcc_trans_enum extends Enum<T_tcc_trans_enum>> {
	static Logger logger = Logger.getLogger(EtfTccTransTemplate.class);

	Class<T_tcc_trans_enum> tccTransEnumClass = (Class<T_tcc_trans_enum>) ((ParameterizedType) getClass()
			.getGenericSuperclass()).getActualTypeArguments()[0];

	EtfTccDao etfTccDao;

	protected EtfTccTransTemplate(EtfTccDao etfTccDao) {
		super();
		this.etfTccDao = etfTccDao;
	}

	public enum TCC_TRANS_STAGE {
		tcc_prepare, tcc_try, tcc_confirm, tcc_cancel;
	}

	public final void executeWithinEtfTcc() {
		TCC_TRANS_STAGE stage = calcCurrTccStage();

		if (stage == TCC_TRANS_STAGE.tcc_prepare) {
			this.exeTccPrepare();
		} else {

			AbstractRedisLockTemplate etfLock = etfTccDao.getEtfTccConcurrentLock(60);

			boolean lockSuccess = etfLock.lock();

			try {
				if (!lockSuccess) {
					String error = "TCC交易[" + getCurrEtfTransExeKey() + "]在" + stage + "阶段获取并发锁失败";
					logger.warn(error);
					throw new EtfRobErr4LockConcurrent(error);
				}

				if (stage == TCC_TRANS_STAGE.tcc_try) {
					this.exeTccTry();
				} else if (stage == TCC_TRANS_STAGE.tcc_confirm) {
					this.exeTccConfirm();
				} else if (stage == TCC_TRANS_STAGE.tcc_cancel) {
					this.exeTccCancel();
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				if (lockSuccess) {
					Long unlock = etfLock.unlock();
					logger.debug("TCC交易[" + getCurrEtfTransExeKey() + "]执行" + stage + "后 释放锁：" + unlock);
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
		String tccTransBizId = EtfTccAop.getTCC_CURR_BIZ_ID();
		String tccEnumClazzName = EtfTccAop.getTCC_CURR_TRANS_ENUM_CLAZZ_NAME();
		try {
			tccCancel();

			String key = etfTccDao.popTccCancelCountorListOnFinished(tccEnumClazzName, tccTransBizId);
			if (key == null) {
				logger.debug("popTccCancelListOnCancelFinished返回null，表明当前所有TCC交易都已经cancel完成，可以标记整个交易canceled");
				etfTccDao.pushTccCanceledList(tccEnumClazzName, tccTransBizId);
			}
		} catch (Exception e) {
			logger.error("TCC[" + getCurrEtfTransExeKey() + "]cancel失败" + e.getMessage(), e);
			etfTccDao.popTccCancelCountorAndFlagFailure(tccEnumClazzName, tccTransBizId, e);
		}
	}

	private void exeTccConfirm() {
		String tccTransBizId = EtfTccAop.getTCC_CURR_BIZ_ID();
		String tccEnumClazzName = EtfTccAop.getTCC_CURR_TRANS_ENUM_CLAZZ_NAME();
		try {
			tccConfirm();

			String key = etfTccDao.popTccConfirmCountorListOnSuccess(tccEnumClazzName, tccTransBizId);
			if (key == null) {
				logger.debug("popTccConfirmListOnSuccess返回null，表明当前所有TCC交易都已经confirm完成，可以标记整个交易success");
				etfTccDao.pushTccSuccessList(tccEnumClazzName, tccTransBizId);
			}
		} catch (Exception e) {
			logger.error("TCC[" + getCurrEtfTransExeKey() + "]confirm失败" + e.getMessage(), e);
			etfTccDao.popTccConfirmCountorAndFlagFailure(tccEnumClazzName, tccTransBizId, e);
		}
	}

	private void exeTccTry() {
		String tccTransBizId = EtfTccAop.getTCC_CURR_BIZ_ID();
		String transTypeEnumClazz = EtfTccAop.getTCC_CURR_TRANS_ENUM_CLAZZ_NAME();
		String transTypeEnumValue = EtfTccAop.getTCC_CURR_ENUM_VALUE();
		try {
			tccTry();

			String key = etfTccDao.popTccTryCountorListOnSuccess(tccTransBizId, transTypeEnumClazz);
			if (key == null) {
				logger.debug("popTccTransListOnTrySuccess返回null，表明当前所有TCC交易都已经try完成，可以触发confirm或cancel了");
				etfTccDao.triggerTccConfirmOrCancel(tccTransBizId, transTypeEnumClazz);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			etfTccDao.updateTccErrorDetail(tccTransBizId, transTypeEnumClazz, transTypeEnumValue, e);
			etfTccDao.popTccTransListAndFlagTccFailure(tccTransBizId, transTypeEnumClazz, transTypeEnumValue);
		} finally {

		}
	}

	private TCC_TRANS_STAGE calcCurrTccStage() {
		return EtfTccAop.getCurrTccStage();
	}

	private String getCurrEtfTransExeKey() {
		String bizId = EtfTccAop.getTCC_CURR_BIZ_ID();
		String transTypeEnumClazz = EtfTccAop.getTCC_CURR_TRANS_ENUM_CLAZZ_NAME();
		String transTypeEnumValue = EtfTccAop.getTCC_CURR_ENUM_VALUE();
		return transTypeEnumClazz + "#" + bizId + "@" + transTypeEnumValue;
	}

	protected abstract String calcTccTransBizId();

	/**
	 * 
	 */
	protected abstract void tccTry();

	protected abstract void tccConfirm();

	protected abstract void tccCancel();

}