package cn.panshi.etf.core;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
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
		ETF_LOCKER,
		/**
		 * key示例 ETF_TRANS_RECORD:com.xxx.EtfEnum@transA#bizId123 value示例
		 * {"tr":{},"trLogJsonArry":[{},{}]
		 */
		ETF_TRANS_RECORD,
		/**
		 * redis中此前缀的key充当了retry重试任务的定时器： 
		 * --为其设置过期时间，设为希望重试任务执行的时间
		 * --然后监听__keyevent@0__:expired，在redis过期清理事件中找到此前缀的key 然后为其执行重试操作
		 * --因为过期时value以找不到，因此要求key中包含重试所需的全部信息：etfEnumClass、etfTransType、bizId
		 */
		ETF_FAILURE_RETRY_TIMER,
		/**
		 * 前缀的key用于存储重试任务，不设过期时间，
		 * 在设置重试timer时同时保存，在timer触发重试任务执行后删除；
		 * 此类型key的作用是提高交易重试机制的可靠性： 
		 * 当重试timer因故未触发重试（例如timer到期时没有监听器在运行）而导致重试停滞， 还有机会通过一个schedule轮询此队列来补救
		 */
		ETF_FAILURE_RETRY_QUEUE,
		/**
		 * 跟ETF_FAILURE_RETRY_TIMER类似，在redis中此前缀的key充当了transQuery任务的定时器
		 */
		ETF_TRANS_QUERY_TIMER,
		/**
		 * 跟ETF_FAILURE_RETRY_QUEUE类似，用于提高交易查询机制的可靠性
		 */
		ETF_TRANS_QUERY_QUEUE;
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
		return ETF_REDIS_KEYS.ETF_LOCKER + ":" + etfTransTypeEnumClass + "@" + etfTransTypeEnumValue + "#" + bizId;
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
		return ETF_REDIS_KEYS.ETF_TRANS_RECORD + ":" + transTypeEnumClazz + "@" + transType + "#" + bizId;
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
	public void insertEtfRetryQueueAndTimer(EtfTransRecord tr) {
		String retryTime = new SimpleDateFormat("yyyyMMdd_HHmm").format(tr.getNextRetryTime());
		String key4Timer = ETF_REDIS_KEYS.ETF_FAILURE_RETRY_TIMER + ":" + retryTime + ":" + tr.getTransTypeEnumClazz()
				+ "@" + tr.getTransType() + "#" + tr.getBizId();
		String key4Queue = ETF_REDIS_KEYS.ETF_FAILURE_RETRY_QUEUE + ":" + retryTime + ":" + tr.getTransTypeEnumClazz()
				+ "@" + tr.getTransType() + "#" + tr.getBizId();
		List<Object> txResults = (List<Object>) redisTemplate.execute(new SessionCallback<List<Object>>() {
			@Override
			public List<Object> execute(RedisOperations operations) throws DataAccessException {
				operations.multi();

				operations.opsForValue().set(key4Timer, "");
				operations.expireAt(key4Timer, tr.getNextRetryTime());

				operations.opsForValue().set(key4Queue, "");

				return operations.exec();
			}
		});

		logger.debug("事务执行结果：" + txResults.size());
	}

	@Override
	public void addTrTransLog(EtfTransRecord tr, EtfTransExeLog etfLog) {
		EtfTransRecord po = loadEtfTransRecord(tr);
		po.getLogList().add(etfLog);
		saveTransRecord(po);
	}

	@Override
	public void insertEtfQueryQueueAndTimer(EtfTransRecord tr) {
		String queryTime = new SimpleDateFormat("yyyyMMdd_HHmm").format(tr.getNextQueryTime());
		String key = ETF_REDIS_KEYS.ETF_TRANS_QUERY_TIMER + ":" + queryTime + ":" + tr.getTransTypeEnumClazz() + "@"
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
		EtfTransExeLog etfLog = new EtfTransExeLog();
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

	public Set<String> listFailureRetryQueue() {
		return redisTemplate.keys(ETF_REDIS_KEYS.ETF_FAILURE_RETRY_QUEUE.name() + "*");
	}

	@Override
	public void deleteEtfRetryQueueByTimerKey(String currEtfTransRetryTimerKey) {
		String keySurfix = currEtfTransRetryTimerKey.substring(ETF_REDIS_KEYS.ETF_FAILURE_RETRY_TIMER.name().length());

		String retryQueueKey = ETF_REDIS_KEYS.ETF_FAILURE_RETRY_QUEUE + keySurfix;
		redisTemplate.delete(retryQueueKey);
	}

	@Override
	public void deleteEtfQueryQueueByTimerKey(String currEtfTransQueryTimerKey) {
		String keySurfix = currEtfTransQueryTimerKey.substring(ETF_REDIS_KEYS.ETF_TRANS_QUERY_TIMER.name().length());

		String queryQueueKey = ETF_REDIS_KEYS.ETF_TRANS_QUERY_QUEUE + keySurfix;
		redisTemplate.delete(queryQueueKey);
	}
}