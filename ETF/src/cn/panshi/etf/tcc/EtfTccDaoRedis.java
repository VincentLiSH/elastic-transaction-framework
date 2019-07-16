package cn.panshi.etf.tcc;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import cn.panshi.etf.core.EtfAbstractRedisLockTemplate;
import cn.panshi.etf.tcc.EtfTccTransTemplate.TCC_TRANS_STAGE;

@Component
@SuppressWarnings({ "rawtypes", "unchecked" })
public class EtfTccDaoRedis implements EtfTccDao {
	static Logger logger = Logger.getLogger(EtfTccDaoRedis.class);
	@Resource
	RedisTemplate redisTemplate;
	@Resource
	ThreadPoolTaskExecutor executor;
	@Resource
	EtfTccBeanUtil etfTccBeanUtil;

	public enum ETF_TCC_KEYS {
		ETF_TCC_LOCK, //
		ETF_TCC_STEP, //
		ETF_TCC_COUNTOR_LIST_TRY, //try计数器，当最后一个try完成时返回null 于是触发所有交易confirm或cancel
		ETF_TCC_COUNTOR_LIST_CONFIRM, //confirm计数器，当最后一个confirm完成时返回null 于是触发TCC完成
		ETF_TCC_COUNTOR_LIST_CANCEL, //
		ETF_TCC_FAILURE_FLAG_LIST;//整个TCC事务失败的标志位

	}

	@Override
	public EtfTccStep loadTccTransRecordStep(String transTypeEnumClazz, String transType, String bizId) {
		String key = calcTccRecordStepKey(transTypeEnumClazz, transType, bizId);

		return (EtfTccStep) redisTemplate.opsForValue().get(key);
	}

	private String calcTccRecordStepKey(String tccTransEnumClazz, String tccTransEnumValue, String bizId) {
		return ETF_TCC_KEYS.ETF_TCC_STEP + ":" + tccTransEnumClazz + ":#" + bizId + ":@" + tccTransEnumValue;
	}

	@Override
	public void saveEtfTccStep(String tccEnumClassName, String bizId, String tccEnumValue, String bizStateJson) {
		EtfTccStep step = new EtfTccStep();
		step.setCrtDate(new Date());
		step.setTccEnumValue(tccEnumValue);
		step.setBizStateJson(bizStateJson);

		String key = calcTccRecordStepKey(tccEnumClassName, tccEnumValue, bizId);
		redisTemplate.opsForValue().set(key, step);
	}

	/**
	 * memo:初始化Tcc try计数器，以便TCC交易并发执行到最后一个try完成后 触发confirm或cancel
	 */
	@Override
	public void initTccCounter4Try(String tccEnumClazzName, String bizId) throws EtfTccException4PrepareStage {
		try {
			Enum[] enumConstants = ((Class<Enum>) Class.forName(tccEnumClazzName)).getEnumConstants();
			logger.debug("初始化Tcc[" + tccEnumClazzName + "#" + bizId + "]try计数器：" + (enumConstants.length - 1)
					+ "，以便TCC交易并发执行到最后一个try完成后 触发confirm或cancel");

			String tccTryCounterKey = calcTccCountor4TryKey(tccEnumClazzName, bizId);
			Long countorListSize = redisTemplate.opsForList().size(tccTryCounterKey);
			if (countorListSize == null || countorListSize == 0L) {
				for (int i = 0; i < enumConstants.length - 1; i++) {
					redisTemplate.opsForList().leftPush(tccTryCounterKey, "" + (i + 1));
				}
			} else {
				logger.warn("重复初始化Tcc try计数器：" + tccEnumClazzName + "#" + bizId);
			}
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new EtfTccException4PrepareStage(e.getMessage());
		}
	}

	@Override
	public void startTccTransAsynch(String transTypeEnumClazz, String tccTransEnumValue, String tccTransBizId) {
		executor.submit(new Runnable() {
			@Override
			public void run() {
				EtfTccStep tr = loadTccTransRecordStep(transTypeEnumClazz, tccTransEnumValue, tccTransBizId);
				JSONObject paramJsonObj = JSONObject.parseObject(tr.getBizStateJson());

				EtfTccAop.setTccCurrStageTry();
				EtfTccAop.setTCC_CURR_BIZ_ID(tccTransBizId);
				etfTccBeanUtil.invokeTccBean(transTypeEnumClazz, tccTransEnumValue, paramJsonObj);
			}
		});

	}

	@Override
	public EtfAbstractRedisLockTemplate getEtfTccConcurrentLock(int lockAutoExpireSeconds) {
		return new EtfAbstractRedisLockTemplate(redisTemplate, lockAutoExpireSeconds, UUID.randomUUID().toString()) {
			@Override
			protected String constructKey() {
				TCC_TRANS_STAGE currTccStage = EtfTccAop.getCurrTccStage();
				String etfTransTypeEnumClass = EtfTccAop.getTCC_CURR_TRANS_ENUM_CLAZZ_NAME();
				String etfTransTypeEnumValue = EtfTccAop.getTCC_CURR_ENUM_VALUE();
				String bizId = EtfTccAop.getTCC_CURR_BIZ_ID();

				return ETF_TCC_KEYS.ETF_TCC_LOCK + ":" + etfTransTypeEnumClass + ":#" + bizId + ":@"
						+ etfTransTypeEnumValue + "$" + currTccStage;
			}
		};
	}

	@Override
	public String popTccTransListOnTrySuccess(String currBizId, String currTccTransEnumClazzName) {
		String tccTryListKey = calcTccCountor4TryKey(currTccTransEnumClazzName, currBizId);
		Object popValue = redisTemplate.opsForList().rightPop(tccTryListKey);
		return (String) popValue;
	}

	@Override
	public void popTccTransListAndFlagTccFailure(String tccTransBizId, String transTypeEnumClazz,
			String transTypeEnumValue) {
		String tccTryListKey = calcTccCountor4TryKey(transTypeEnumClazz, tccTransBizId);
		String tccFailureFlagListKey = this.calcTccFailureFlagListKey(transTypeEnumClazz, tccTransBizId);

		//		redisTemplate.opsForValue().set(tccFailureFlagKey,
		//				"The whole TCC" + " was flag as failure for step " + transTypeEnumValue);

		Object result = redisTemplate.opsForList().rightPopAndLeftPush(tccTryListKey, tccFailureFlagListKey);
		if (result == null) {
			logger.info("最后一个try失败，需要触发所有相关交易cancel");

			triggerTccCancel(tccTransBizId, transTypeEnumClazz);
		}

	}

	private String calcTccFailureFlagListKey(String transTypeEnumClazz, String tccTransBizId) {
		return ETF_TCC_KEYS.ETF_TCC_FAILURE_FLAG_LIST + ":" + transTypeEnumClazz + ":#" + tccTransBizId;
	}

	protected String calcTccCountor4TryKey(String tccTransEnumClazzName, String bizId) {
		String tccTryListKey = ETF_TCC_KEYS.ETF_TCC_COUNTOR_LIST_TRY + ":" + tccTransEnumClazzName + ":#" + bizId;
		return tccTryListKey;
	}

	@Override
	public void triggerTccConfirmOrCancel(String tccTransBizId, String transTypeEnumClazz) {
		String failureFlagListKey = this.calcTccFailureFlagListKey(transTypeEnumClazz, tccTransBizId);
		Object failureStep = redisTemplate.opsForList().leftPop(failureFlagListKey);
		if (failureStep != null) {
			logger.info("TCC交易[" + transTypeEnumClazz + "#" + tccTransBizId + "]try阶段存在failure step，触发整个交易撤销！");
			triggerTccCancel(tccTransBizId, transTypeEnumClazz);
		} else {
			triggerTccConfirm(tccTransBizId, transTypeEnumClazz);
		}
	}

	private void triggerTccConfirm(String tccTransBizId, String transTypeEnumClazz) {
		logger.debug("Triggering TCC[" + transTypeEnumClazz + "#" + tccTransBizId + "] to confirm...");

		List<EtfTccStep> trStepList = queryTccRecordStepList(transTypeEnumClazz, tccTransBizId);
		logger.debug(
				"查找到需要confirm的TCC[" + transTypeEnumClazz + "#" + tccTransBizId + "] step" + trStepList.size() + "个");
		this.initTccCounter4Confirm(transTypeEnumClazz, tccTransBizId, trStepList.size() - 1);

		for (EtfTccStep step : trStepList) {
			executor.submit(new Runnable() {
				@Override
				public void run() {
					EtfTccStep tr = loadTccTransRecordStep(transTypeEnumClazz, step.getTccEnumValue(), tccTransBizId);
					JSONObject paramJsonObj = JSONObject.parseObject(tr.getBizStateJson());

					EtfTccAop.setTccCurrStageConfirm();
					EtfTccAop.setTCC_CURR_BIZ_ID(tccTransBizId);
					etfTccBeanUtil.invokeTccBean(transTypeEnumClazz, step.getTccEnumValue(), paramJsonObj);
				}
			});
		}
	}

	private void initTccCounter4Confirm(String tccEnumClazzName, String tccTransBizId, int countorInitValue) {
		logger.debug("初始化Tcc[" + tccEnumClazzName + "#" + tccTransBizId + "]confirm计数器：" + countorInitValue
				+ "，以便TCC交易并发执行到最后一个confirm完成后 触发后续动作");

		String tccConfirmCounterKey = calcTccCountor4ConfirmKey(tccEnumClazzName, tccTransBizId);
		Long countorListSize = redisTemplate.opsForList().size(tccConfirmCounterKey);
		if (countorListSize == null || countorListSize == 0L) {
			for (int i = 0; i < countorInitValue; i++) {
				redisTemplate.opsForList().leftPush(tccConfirmCounterKey, "" + (i + 1));
			}
		} else {
			logger.warn("重复初始化Tcc confirm计数器：" + tccEnumClazzName + "#" + tccTransBizId);
		}
	}

	private String calcTccCountor4ConfirmKey(String tccEnumClazzName, String bizId) {
		return ETF_TCC_KEYS.ETF_TCC_COUNTOR_LIST_CONFIRM + ":" + tccEnumClazzName + ":#" + bizId;
	}

	private void triggerTccCancel(String tccTransBizId, String transTypeEnumClazz) {
		logger.debug("Triggering TCC[" + transTypeEnumClazz + "#" + tccTransBizId + "] to cancel...");

		List<EtfTccStep> trStepList = queryTccRecordStepList(transTypeEnumClazz, tccTransBizId);
		logger.debug(
				"查找到需要cancel的TCC[" + transTypeEnumClazz + "#" + tccTransBizId + "] step" + trStepList.size() + "个");
		this.initTccCounter4Cancel(transTypeEnumClazz, tccTransBizId, trStepList.size() - 1);
		for (EtfTccStep step : trStepList) {
			executor.submit(new Runnable() {
				@Override
				public void run() {
					EtfTccStep tr = loadTccTransRecordStep(transTypeEnumClazz, step.getTccEnumValue(), tccTransBizId);
					JSONObject paramJsonObj = JSONObject.parseObject(tr.getBizStateJson());

					EtfTccAop.setTccCurrStageCancel();
					EtfTccAop.setTCC_CURR_BIZ_ID(tccTransBizId);
					etfTccBeanUtil.invokeTccBean(transTypeEnumClazz, step.getTccEnumValue(), paramJsonObj);
				}
			});
		}
	}

	private void initTccCounter4Cancel(String tccEnumClazzName, String tccTransBizId, int countorInitValue) {
		logger.debug("初始化Tcc[" + tccEnumClazzName + "#" + tccTransBizId + "] confirm计数器：" + countorInitValue
				+ "，以便TCC交易并发执行到最后一个cancel完成后 触发后续动作");

		String tccConfirmCancelKey = calcTccCountor4CancelKey(tccEnumClazzName, tccTransBizId);
		Long countorListSize = redisTemplate.opsForList().size(tccConfirmCancelKey);
		if (countorListSize == null || countorListSize == 0L) {
			for (int i = 0; i < countorInitValue; i++) {
				redisTemplate.opsForList().leftPush(tccConfirmCancelKey, "" + (i + 1));
			}
		} else {
			logger.warn("重复初始化Tcc cancel计数器：" + tccEnumClazzName + "#" + tccTransBizId);
		}
	}

	private String calcTccCountor4CancelKey(String tccEnumClazzName, String bizId) {
		return ETF_TCC_KEYS.ETF_TCC_COUNTOR_LIST_CANCEL + ":" + tccEnumClazzName + ":#" + bizId;
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

	private List<EtfTccStep> queryTccRecordStepList(String transTypeEnumClazz, String bizId) {
		String recordKeyPrefix = ETF_TCC_KEYS.ETF_TCC_STEP + ":" + transTypeEnumClazz + ":#" + bizId + ":@*";
		Set<String> keys = redisTemplate.keys(recordKeyPrefix);
		return redisTemplate.opsForValue().multiGet(keys);
	}

}