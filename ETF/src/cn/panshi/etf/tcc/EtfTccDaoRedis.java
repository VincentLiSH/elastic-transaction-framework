package cn.panshi.etf.tcc;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import cn.panshi.etf.core.EtfAbstractRedisLockTemplate;

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
		ETF_TCC_PREPARE_SET, //
		ETF_TCC_TRY_LIST, //
		ETF_TCC_CONFIRM_LIST, //
		ETF_TCC_RECORD, //
		ETF_TCC_TIMER_TRY, //
		ETF_TCC_TIMER_CONFIRM, //
		ETF_TCC_TIMER_CANCEL, //
		ETF_TCC_COUNTOR_LIST_TRY, //try计数器，当最后一个try完成时返回null 于是触发所有交易confirm或cancel
		ETF_TCC_COUNTOR_LIST_CONFIRM, //confirm计数器，当最后一个confirm完成时返回null 于是触发TCC完成
		ETF_TCC_FAILURE_FLAG;//整个TCC事务失败的标志位

	}

	@Override
	public EtfTccRecordStep loadTccTransRecordStep(String transTypeEnumClazz, String transType, String bizId) {
		String key = calcTccRecordStepKey(transTypeEnumClazz, transType, bizId);

		return (EtfTccRecordStep) redisTemplate.opsForValue().get(key);
	}

	private String calcTccRecordStepKey(String tccTransEnumClazz, String tccTransEnumValue, String bizId) {
		return ETF_TCC_KEYS.ETF_TCC_RECORD + ":" + tccTransEnumClazz + "#" + bizId + "@" + tccTransEnumValue;
	}

	@Override
	public void saveEtfTccRecordStep(String tccEnumClassName, String bizId, String tccEnumValue, String bizStateJson) {
		EtfTccRecordStep step = new EtfTccRecordStep();
		step.setCrtDate(new Date());
		step.setTccEnumValue(tccEnumValue);
		step.setBizStateJson(bizStateJson);

		String key = calcTccRecordStepKey(tccEnumClassName, tccEnumValue, bizId);
		redisTemplate.opsForValue().set(key, step);
	}

	@Override
	public boolean addEtfTccTransPrepareList(String tccEnumClassName, String bizId, String tccEnumValue)
			throws EtfTccException4PrepareStage {
		String tccPrepareListKey = ETF_TCC_KEYS.ETF_TCC_PREPARE_SET + ":" + tccEnumClassName + "#" + bizId;
		boolean checkExist = redisTemplate.opsForSet().isMember(tccPrepareListKey, tccEnumValue);
		if (checkExist) {
			return false;
		} else {
			Long added = redisTemplate.opsForSet().add(tccPrepareListKey, tccEnumValue);
			logger.debug("add tcc prepare set success,return " + added);

			this.initTccCounter4Try(tccEnumClassName, bizId);
			return true;
		}
	}

	/**
	 * memo:初始化Tcc try计数器，以便TCC交易并发执行到最后一个try完成后 触发confirm或cancel
	 */
	private void initTccCounter4Try(String tccEnumClassName, String bizId) throws EtfTccException4PrepareStage {
		try {
			Enum[] enumConstants = ((Class<Enum>) Class.forName(tccEnumClassName)).getEnumConstants();
			logger.debug("初始化Tcc try计数器：" + (enumConstants.length - 1) + "，以便TCC交易并发执行到最后一个try完成后 触发confirm或cancel");

			String key = ETF_TCC_KEYS.ETF_TCC_COUNTOR_LIST_TRY + ":" + tccEnumClassName + "#" + bizId;
			Long countorListSize = redisTemplate.opsForList().size(key);
			if (countorListSize == null || countorListSize == 0L) {
				for (int i = 0; i < enumConstants.length - 1; i++) {
					redisTemplate.opsForList().leftPush(key, "" + (i + 1));
				}
			}

		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new EtfTccException4PrepareStage(e.getMessage());
		}
	}

	@Override
	public Set<String> findTccTransList2Start(String tccEnumClassName, String bizId) {
		return redisTemplate.opsForSet()
				.members(ETF_TCC_KEYS.ETF_TCC_PREPARE_SET + ":" + tccEnumClassName + "#" + bizId);
	}

	@Override
	public void startTccTransByPreparedKey(String transTypeEnumClazz, String tccTransEnumValue, String tccTransBizId) {
		executor.submit(new Runnable() {
			@Override
			public void run() {
				EtfTccRecordStep tr = loadTccTransRecordStep(transTypeEnumClazz, tccTransEnumValue, tccTransBizId);
				JSONObject paramJsonObj = JSONObject.parseObject(tr.getBizStateJson());

				EtfTccAop.setCurrTccTryStage();
				EtfTccAop.setCURR_INVOKE_BIZ_ID(tccTransBizId);
				EtfTccAop.setCURR_INVOKE_TCC_ENUM_CLAZZ_NAME(transTypeEnumClazz);
				EtfTccAop.setCURR_INVOKE_TCC_ENUM_VALUE(tccTransEnumValue);
				etfTccBeanUtil.invokeEtfBean(transTypeEnumClazz, tccTransEnumValue, paramJsonObj);
			}
		});

	}

	@Override
	public EtfAbstractRedisLockTemplate getEtfTccConcurrentLock(int expireSeconds) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String popTccTransListOnTrySuccess(String currBizId, String currTccTransEnumClazzName) {
		String tccTryListKey = calcTccCountorList4TryKey(currTccTransEnumClazzName, currBizId);
		Object popValue = redisTemplate.opsForList().rightPop(tccTryListKey);
		return (String) popValue;
	}

	@Override
	public void popTccTransListAndFlagTccFailure(String tccTransBizId, String transTypeEnumClazz,
			String transTypeEnumValue) {
		String tccFailureFlagKey = this.calcTccFailureFlagKey(transTypeEnumClazz, tccTransBizId);
		redisTemplate.opsForValue().set(tccFailureFlagKey,
				"The whole TCC" + " was flag as failure for step " + transTypeEnumValue);

		String tccTryListKey = calcTccCountorList4TryKey(transTypeEnumClazz, tccTransBizId);
		Object popValue = redisTemplate.opsForList().rightPop(tccTryListKey);
		if (popValue == null) {
			logger.info("最后一个try失败，需要触发所有相关交易cancel");

			triggerTccCancel(tccTransBizId, transTypeEnumClazz);
		}

	}

	private String calcTccFailureFlagKey(String transTypeEnumClazz, String tccTransBizId) {
		return ETF_TCC_KEYS.ETF_TCC_FAILURE_FLAG + ":" + transTypeEnumClazz + "#" + tccTransBizId;
	}

	protected String calcTccCountorList4TryKey(String tccTransEnumClazzName, String bizId) {
		String tccTryListKey = ETF_TCC_KEYS.ETF_TCC_COUNTOR_LIST_TRY + ":" + tccTransEnumClazzName + "#" + bizId;
		return tccTryListKey;
	}

	@Override
	public void triggerTccConfirmOrCancel(String tccTransBizId, String transTypeEnumClazz) {
		logger.debug("Triggering TCC[" + transTypeEnumClazz + "#" + tccTransBizId + "] to confirm or cancel...");
		String failureFlagKey = this.calcTccFailureFlagKey(transTypeEnumClazz, tccTransBizId);
		if (redisTemplate.opsForValue().get(failureFlagKey) != null) {
			triggerTccCancel(tccTransBizId, transTypeEnumClazz);
		} else {
			triggerTccConfirm(tccTransBizId, transTypeEnumClazz);
		}

	}

	private void triggerTccConfirm(String tccTransBizId, String transTypeEnumClazz) {
		String confirmTimerKey = this.calcTccConfirmTimerKey(transTypeEnumClazz, tccTransBizId);
		redisTemplate.opsForValue().set(confirmTimerKey, "", 1, TimeUnit.SECONDS);

		logger.debug("设置timer[" + confirmTimerKey + "]过期");
	}

	private String calcTccConfirmTimerKey(String transTypeEnumClazz, String tccTransBizId) {
		return ETF_TCC_KEYS.ETF_TCC_TIMER_CONFIRM + ":" + transTypeEnumClazz + "#" + tccTransBizId;
	}

	private void triggerTccCancel(String tccTransBizId, String transTypeEnumClazz) {
		String cancelTimerKey = this.calcTccCancelTimerKey(transTypeEnumClazz, tccTransBizId);
		redisTemplate.opsForValue().set(cancelTimerKey, "", 1, TimeUnit.SECONDS);

		logger.debug("设置timer[" + cancelTimerKey + "]过期");
	}

	private String calcTccCancelTimerKey(String transTypeEnumClazz, String tccTransBizId) {
		return ETF_TCC_KEYS.ETF_TCC_TIMER_CANCEL + ":" + transTypeEnumClazz + "#" + tccTransBizId;
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

	@Override
	public List<EtfTccRecordStep> queryTccRecordStepList(String transTypeEnumClazz, String bizId) {
		String recordKeyPrefix = ETF_TCC_KEYS.ETF_TCC_RECORD + ":" + transTypeEnumClazz + "#" + bizId + "@*";
		Set<String> keys = redisTemplate.keys(recordKeyPrefix);
		return redisTemplate.opsForValue().multiGet(keys);
	}

}