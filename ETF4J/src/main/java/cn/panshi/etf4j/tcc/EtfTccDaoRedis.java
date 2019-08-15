package cn.panshi.etf4j.tcc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
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

import cn.panshi.etf4j.RedisUtil;
import cn.panshi.etf4j.robust.EtfAbstractRedisLockTemplate;
import cn.panshi.etf4j.tcc.EtfTccTransTemplate.TCC_TRANS_STAGE;

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
		ETF_TCC_FAILURE_FLAG_LIST, //整个TCC事务失败的标志位
		ETF_TCC_FAILURE_2_CONFIRM_DETAIL_LIST, //
		ETF_TCC_FAILURE_2_CANCEL_DETAIL_LIST, //
		ETF_TCC_SUCCESS_LIST, //
		ETF_TCC_CANCELED_LIST;
	}

	@Override
	public EtfTccStep loadTccTransRecordStep(String tccEnumClazzName, String transType, String bizId) {
		String key = calcTccRecordStepKey(tccEnumClazzName, transType, bizId);

		return (EtfTccStep) redisTemplate.opsForValue().get(key);
	}

	private String calcTccRecordStepKey(String tccTransEnumClazz, String tccTransEnumValue, String bizId) {
		return ETF_TCC_KEYS.ETF_TCC_STEP + ":" + tccTransEnumClazz + ":#" + bizId + ":@" + tccTransEnumValue;
	}

	@Override
	public void saveNewEtfTccStep(String tccEnumClassName, String bizId, String tccEnumValue, String bizStateJson)
			throws EtfTccException4PrepareStage {
		EtfTccStep step = new EtfTccStep();
		step.setCrtDate(new Date());
		step.setTccEnumValue(tccEnumValue);
		step.setBizStateJson(bizStateJson);

		String key = calcTccRecordStepKey(tccEnumClassName, tccEnumValue, bizId);

		Boolean setIfAbsent = redisTemplate.opsForValue().setIfAbsent(key, step);
		if (setIfAbsent == null || !setIfAbsent) {
			throw new EtfTccException4PrepareStage("[" + key + "]对应的TCC交易已经存在");
		}
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
	public void startTccTransAsynch(String tccEnumClazzName, String tccTransEnumValue, String tccTransBizId) {
		executor.submit(new Runnable() {
			@Override
			public void run() {
				EtfTccStep tr = loadTccTransRecordStep(tccEnumClazzName, tccTransEnumValue, tccTransBizId);
				JSONObject paramJsonObj = JSONObject.parseObject(tr.getBizStateJson());

				EtfTccAop.setTccCurrStageTry();
				EtfTccAop.setTCC_CURR_BIZ_ID(tccTransBizId);
				etfTccBeanUtil.invokeTccBean(tccEnumClazzName, tccTransEnumValue, paramJsonObj);
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
	public String popTccTryCountorListOnSuccess(String currBizId, String currTccTransEnumClazzName) {
		String tccTryListKey = calcTccCountor4TryKey(currTccTransEnumClazzName, currBizId);
		Object popValue = redisTemplate.opsForList().rightPop(tccTryListKey);
		return (String) popValue;
	}

	@Override
	public void popTccTransListAndFlagTccFailure(String tccTransBizId, String tccEnumClazzName,
			String transTypeEnumValue) {
		String tccTryListKey = calcTccCountor4TryKey(tccEnumClazzName, tccTransBizId);
		String tccFailureFlagListKey = this.calcTccFailureFlagListKey(tccEnumClazzName, tccTransBizId);

		Object result = RedisUtil.rightPopAndLeftPushAnotherValue(redisTemplate, tccTryListKey, tccFailureFlagListKey,
				transTypeEnumValue); //redisTemplate.opsForList().rightPopAndLeftPush();
		logger.info("TCC交易[" + tccEnumClazzName + "#" + tccTransBizId + "]try失败，标记在" + tccFailureFlagListKey);
		if (result == null) {
			logger.info("最后一个步骤try失败，直接触发整个交易cancel");
			//redisTemplate.opsForList().leftPush(tccFailureFlagListKey, transTypeEnumValue);
			triggerTccCancel(tccTransBizId, tccEnumClazzName);
		}

	}

	private String calcTccFailureFlagListKey(String tccEnumClazzName, String tccTransBizId) {
		return ETF_TCC_KEYS.ETF_TCC_FAILURE_FLAG_LIST + ":" + tccEnumClazzName + ":#" + tccTransBizId;
	}

	protected String calcTccCountor4TryKey(String tccTransEnumClazzName, String bizId) {
		String tccTryListKey = ETF_TCC_KEYS.ETF_TCC_COUNTOR_LIST_TRY + ":" + tccTransEnumClazzName + ":#" + bizId;
		return tccTryListKey;
	}

	@Override
	public void triggerTccConfirmOrCancel(String tccTransBizId, String tccEnumClazzName) {
		List failureList = queryFailureFlagList(tccTransBizId, tccEnumClazzName);
		if (failureList != null && failureList.size() > 0) {
			logger.info("TCC交易[" + tccEnumClazzName + "#" + tccTransBizId + "]最后一个step完成try，存在failure["
					+ failureList.size() + "个]，开始触发整个交易撤销...");
			triggerTccCancel(tccTransBizId, tccEnumClazzName);
		} else {
			logger.info("TCC交易[" + tccEnumClazzName + "#" + tccTransBizId + "]最后一个step完成try阶段，没有任何异常，开始触发整个交易确认...");
			triggerTccConfirm(tccTransBizId, tccEnumClazzName);
		}
	}

	private void triggerTccConfirm(String tccTransBizId, String tccEnumClazzName) {
		logger.debug("Triggering TCC[" + tccEnumClazzName + "#" + tccTransBizId + "] to confirm...");

		List<EtfTccStep> trStepList = queryTccRecordStepList(tccEnumClazzName, tccTransBizId);
		logger.debug("查找到需要confirm的TCC[" + tccEnumClazzName + "#" + tccTransBizId + "] step" + trStepList.size() + "个");
		this.initTccCounter4Confirm(tccEnumClazzName, tccTransBizId, trStepList.size() - 1);

		for (EtfTccStep step : trStepList) {
			executor.submit(new Runnable() {
				@Override
				public void run() {
					EtfTccStep tr = loadTccTransRecordStep(tccEnumClazzName, step.getTccEnumValue(), tccTransBizId);
					JSONObject paramJsonObj = JSONObject.parseObject(tr.getBizStateJson());

					EtfTccAop.setTccCurrStageConfirm();
					EtfTccAop.setTCC_CURR_BIZ_ID(tccTransBizId);
					etfTccBeanUtil.invokeTccBean(tccEnumClazzName, step.getTccEnumValue(), paramJsonObj);
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

	private void triggerTccCancel(String tccTransBizId, String tccEnumClazzName) {
		logger.debug("Triggering TCC[" + tccEnumClazzName + "#" + tccTransBizId + "] to cancel...");

		List failureList = queryFailureFlagList(tccTransBizId, tccEnumClazzName);

		List<EtfTccStep> trStepList = queryTccRecordStepList(tccEnumClazzName, tccTransBizId);

		logger.debug("TCC[" + tccEnumClazzName + "#" + tccTransBizId + "]包含[" + trStepList.size()
				+ "]个step,try阶段已经failure的step[" + failureList.size() + "]个");
		if (trStepList.size() <= failureList.size()) {
			logger.info("所有step都try failure，无需cancel，直接返回！");
			this.pushTccCanceledList(tccEnumClazzName, tccTransBizId);
			return;
		}

		this.initTccCounter4Cancel(tccEnumClazzName, tccTransBizId, (trStepList.size() - failureList.size() - 1));

		for (EtfTccStep step : trStepList) {
			if (failureList.contains(step.getTccEnumValue())) {
				continue;
			}
			executor.submit(new Runnable() {
				@Override
				public void run() {
					EtfTccStep tr = loadTccTransRecordStep(tccEnumClazzName, step.getTccEnumValue(), tccTransBizId);
					JSONObject paramJsonObj = JSONObject.parseObject(tr.getBizStateJson());

					EtfTccAop.setTccCurrStageCancel();
					EtfTccAop.setTCC_CURR_BIZ_ID(tccTransBizId);
					etfTccBeanUtil.invokeTccBean(tccEnumClazzName, step.getTccEnumValue(), paramJsonObj);
				}
			});
		}
	}

	protected List queryFailureFlagList(String tccTransBizId, String tccEnumClazzName) {
		String failureFlagListKey = this.calcTccFailureFlagListKey(tccEnumClazzName, tccTransBizId);
		return redisTemplate.opsForList().range(failureFlagListKey, 0, -1);
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
	public String popTccCancelCountorListOnFinished(String tccEnumClazzName, String tccTransBizId) {
		String tccCancelListKey = calcTccCountor4CancelKey(tccEnumClazzName, tccTransBizId);
		Object popValue = redisTemplate.opsForList().rightPop(tccCancelListKey);
		return (String) popValue;
	}

	@Override
	public void pushTccCanceledList(String tccEnumClazzName, String bizId) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd_:HH");
		String canceledListKey = ETF_TCC_KEYS.ETF_TCC_CANCELED_LIST + ":" + sdf.format(new Date());

		redisTemplate.opsForList().leftPush(canceledListKey, tccEnumClazzName + "#" + bizId);
		redisTemplate.opsForList().trim(canceledListKey, 0, DEFAULT_CANCELED_LIST_SIZE);
	}

	@Override
	public void popTccCancelCountorAndFlagFailure(String tccEnumClazzName, String tccTransBizId, Exception e) {
		// TODO Auto-generated method stub

	}

	@Override
	public String popTccConfirmCountorListOnSuccess(String tccEnumClazzName, String tccTransBizId) {
		String tccTryListKey = calcTccCountor4ConfirmKey(tccEnumClazzName, tccTransBizId);
		Object popValue = redisTemplate.opsForList().rightPop(tccTryListKey);
		return (String) popValue;
	}

	@Override
	public void pushTccSuccessList(String tccEnumClazzName, String bizId) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd_:HH:mm");
		String successListKey = ETF_TCC_KEYS.ETF_TCC_SUCCESS_LIST + ":" + sdf.format(new Date());

		redisTemplate.opsForList().leftPush(successListKey, tccEnumClazzName + "#" + bizId);
		redisTemplate.opsForList().trim(successListKey, 0, DEFAULT_SUCCESS_LIST_SIZE);
	}

	@Override
	public void popTccConfirmCountorAndFlagFailure(String tccEnumClazzName, String tccTransBizId, Exception ex) {
		String tccConfirmCountorKey = calcTccCountor4ConfirmKey(tccEnumClazzName, tccTransBizId);
		String tccConfirmFailureListKey = this.calcTccFail2ConfirmDetailListKey(tccEnumClazzName, tccTransBizId);

		Object result = redisTemplate.opsForList().rightPopAndLeftPush(tccConfirmCountorKey, tccConfirmFailureListKey);
		if (result == null) {
			redisTemplate.opsForList().leftPush(tccConfirmFailureListKey, "confirm到了最后一个step失败！");
		}
		String timeStamp = new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date());
		redisTemplate.opsForList().leftPush(tccConfirmFailureListKey, timeStamp);

		StringWriter sw = new StringWriter();
		ex.printStackTrace(new PrintWriter(sw));
		String error = sw.toString();
		redisTemplate.opsForList().leftPush(tccConfirmFailureListKey, error);
	}

	private String calcTccFail2ConfirmDetailListKey(String tccEnumClazzName, String tccTransBizId) {
		return ETF_TCC_KEYS.ETF_TCC_FAILURE_2_CONFIRM_DETAIL_LIST + ":" + tccEnumClazzName + ":#" + tccTransBizId;
	}

	private List<EtfTccStep> queryTccRecordStepList(String tccEnumClazzName, String bizId) {
		String recordKeyPrefix = ETF_TCC_KEYS.ETF_TCC_STEP + ":" + tccEnumClazzName + ":#" + bizId + ":@*";
		Set<String> keys = redisTemplate.keys(recordKeyPrefix);
		return redisTemplate.opsForValue().multiGet(keys);
	}

	@Override
	public void updateTccErrorDetail(String bizId, String tccEnumClazzName, String transType, Exception ex) {
		StringWriter sw = new StringWriter();
		ex.printStackTrace(new PrintWriter(sw));
		String error = sw.toString();

		EtfTccStep tccRecord = loadTccTransRecordStep(tccEnumClazzName, transType, bizId);
		tccRecord.setError(error);
		String key = calcTccRecordStepKey(tccEnumClazzName, transType, bizId);

		redisTemplate.opsForValue().set(key, tccRecord);
	}

}