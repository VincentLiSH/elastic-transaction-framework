package cn.panshi.etf.core;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import cn.panshi.etf.core.EtfTransExeLog.TRANS_EXE_MODE;

@Component
@SuppressWarnings({ "rawtypes", "unchecked" })
public class EtfDaoRedis implements EtfDao {
	static Logger logger = LoggerFactory.getLogger(EtfDaoRedis.class);
	@Resource
	RedisTemplate redisTemplate;

	public enum ETF_REDIS_KEYS {
		/**
		 * 
		 */
		ETF_LOCK2,
		/**
		 * key示例   ETF_TRANS_RECORD:com.xxx.EtfEnum@transA#bizId123
		 * value示例  {"tr":{},"trLogJsonArry":[{},{}]
		 */
		ETF_TRANS_RECORD2,
		/**
		 * 
		 */
		ETF_FAILURE_RETRY2,
		/**
		 * 
		 */
		ETF_TRANS_QUERY2;
	}

	@Override
	public EtfAbstractRedisLockTemplate getEtfConcurrentLock(String etfTransTypeEnumClass, String etfTransTypeEnumValue,
			String bizId, int expireSeconds) {
		return new EtfAbstractRedisLockTemplate(redisTemplate, expireSeconds, UUID.randomUUID().toString()) {
			@Override
			protected String constructKey() {
				return genEtfInvokeKey(etfTransTypeEnumClass, etfTransTypeEnumValue, bizId);
			}
		};
	}

	String genEtfInvokeKey(String etfTransTypeEnumClass, String etfTransTypeEnumValue, String bizId) {
		return ETF_REDIS_KEYS.ETF_LOCK2 + ":" + etfTransTypeEnumClass + "@" + etfTransTypeEnumValue + "#" + bizId;
	}

	@Override
	public void validateTransDuplicate(EtfTransRecord tr) throws EtfException4TransDuplicate {
		EtfTransRecord po = loadEtfTransRecord(tr);
		if (po != null) {
			throw new EtfException4TransDuplicate(po);
		}
	}

	@Override
	public EtfTransRecord loadEtfTransRecord(String transTypeEnumClazz, String transType, String bizId) {
		String key = genEtfTrKey(transTypeEnumClazz, transType, bizId);

		return (EtfTransRecord) redisTemplate.opsForValue().get(key);
	}

	@Override
	public String saveTransRecord(EtfTransRecord tr) {
		String key = genEtfTrKey(tr.getTransTypeEnumClazz(), tr.getTransType(), tr.getBizId());

		redisTemplate.opsForValue().set(key, tr);

		if (tr.getTransSuccess() != null && tr.getTransSuccess() && tr.getRetryCount() == null
				&& tr.getQueryCount() == null) {
			int ttl = 3600 * 24;
			logger.debug("ETF交易" + key + "执行无异常，设置过期时间" + ttl + "秒");
			redisTemplate.expire(key, ttl, TimeUnit.SECONDS);//
		}

		return key;
	}

	private String genEtfTrKey(String transTypeEnumClazz, String transType, String bizId) {
		return ETF_REDIS_KEYS.ETF_TRANS_RECORD2 + ":" + transTypeEnumClazz + "@" + transType + "#" + bizId;
	}

	@Override
	public void updateTransRecordNextRetry(EtfTransRecord tr, Date nextRetryTime) {
		tr.setNextRetryTime(nextRetryTime);
		EtfTransRecord po = loadEtfTransRecord(tr.getTransTypeEnumClazz(), tr.getTransType(), tr.getBizId());
		po.setRetryCount(tr.getRetryCount());
		po.setNextRetryTime(nextRetryTime);

		saveTransRecord(po);
	}

	@Override
	public void updateTransRecordRetrySuccess(EtfTransRecord tr, String resultJson) {
		EtfTransRecord po = loadEtfTransRecord(tr);
		po.setTransResultJson(resultJson);
		po.setTransSuccess(true);
		po.setRetryCount(tr.getRetryCount() == null ? 1 : tr.getRetryCount());
		po.setNextRetryTime(null);
		po.setQueryCount(tr.getQueryCount());
		saveTransRecord(po);
	}

	@Override
	public void updateTransRecordMaxRetryTimes(EtfTransRecord tr) {
		EtfTransRecord po = loadEtfTransRecord(tr);
		po.setNextRetryTime(null);
		po.setTransSuccess(false);
		saveTransRecord(po);
	}

	@Override
	public void insertEtfRetryQueue(EtfTransRecord tr) {
		String retryTime = new SimpleDateFormat("yyyyMMdd_HHmm").format(tr.getNextRetryTime());
		String key = ETF_REDIS_KEYS.ETF_FAILURE_RETRY2 + ":" + retryTime + ":" + tr.getTransTypeEnumClazz() + "@"
				+ tr.getTransType() + "#" + tr.getBizId();
		redisTemplate.opsForValue().set(key, "");
		redisTemplate.expireAt(key, tr.getNextRetryTime());
	}

	@Override
	public void addTrTransLog(EtfTransRecord tr, EtfTransExeLog etfLog) {
		EtfTransRecord po = loadEtfTransRecord(tr);
		po.getLogList().add(etfLog);
		saveTransRecord(po);
	}

	@Override
	public void insertEtfQueryQueue(EtfTransRecord tr) {
		String queryTime = new SimpleDateFormat("yyyyMMdd_HHmm").format(tr.getNextQueryTime());
		String key = ETF_REDIS_KEYS.ETF_TRANS_QUERY2 + ":" + queryTime + ":" + tr.getTransTypeEnumClazz() + "@"
				+ tr.getTransType() + "#" + tr.getBizId();
		redisTemplate.opsForValue().set(key, "");
		redisTemplate.expireAt(key, tr.getNextQueryTime());
	}

	@Override
	public void updateTransRecordMaxQueryTimes(EtfTransRecord tr) {
		EtfTransRecord po = loadEtfTransRecord(tr);
		po.setNextQueryTime(null);
		po.setQueryTransSuccess(false);
		saveTransRecord(po);
	}

	@Override
	public void updateTransRecordNextQuery(EtfTransRecord tr, Date nextQueryTime) {
		tr.setNextRetryTime(nextQueryTime);
		EtfTransRecord po = loadEtfTransRecord(tr.getTransTypeEnumClazz(), tr.getTransType(), tr.getBizId());
		po.setQueryCount(tr.getQueryCount());
		po.setNextQueryTime(nextQueryTime);

		saveTransRecord(po);
	}

	@Override
	public void updateTransRecordQuerySuccess(EtfTransRecord tr) {
		EtfTransRecord po = loadEtfTransRecord(tr);
		po.setQueryTransSuccess(true);
		po.setQueryCount(tr.getQueryCount() == null ? 1 : tr.getQueryCount());
		po.setNextQueryTime(null);
		saveTransRecord(po);
	}
	
	@Override
	public void updateTransRecordQueryFailure(EtfTransRecord tr) {
		EtfTransRecord po = loadEtfTransRecord(tr);
		po.setQueryTransSuccess(false);
		po.setQueryCount(tr.getQueryCount() == null ? 1 : tr.getQueryCount());
		po.setNextQueryTime(null);
		saveTransRecord(po);
	}

	@Override
	public void addTransDuplicateInvokeLog(EtfTransRecord tr) {
		EtfTransRecord po = loadEtfTransRecord(tr);
		EtfTransExeLog etfLog=new EtfTransExeLog();
		etfLog.setCrtDate(new Date());
		etfLog.setLogType(TRANS_EXE_MODE.duplicate);
		etfLog.setError(EtfException4TransDuplicate.class.getName());
		po.getLogList().add(etfLog);
		saveTransRecord(po);
	}

	private EtfTransRecord loadEtfTransRecord(EtfTransRecord tr) {
		EtfTransRecord po = loadEtfTransRecord(tr.getTransTypeEnumClazz(), tr.getTransType(), tr.getBizId());
		return po;
	}
}