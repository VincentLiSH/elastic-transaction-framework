package cn.panshi.etf.robust;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import cn.panshi.etf.robust.EtfRobTxRecordLog.TRANS_EXE_MODE;

@Component
@SuppressWarnings({ "rawtypes", "unchecked" })
public class EtfRobDaoRedis implements EtfRobDao {
	static Logger logger = Logger.getLogger(EtfRobDaoRedis.class);
	@Resource
	RedisTemplate redisTemplate;
	@Resource
	EtfRobBeanUtil etfRobBeanUtil;

	public enum ETF_ROB_REDIS_KEYS {
		/**
		 * concurrent lock
		 */
		ETF_ROBUST_LOCKER,
		/**
		 * ETF Robust Tx record
		 */
		ETF_ROBUST_TX_RECORD,
		/**
		 * 交易记录销毁计时器
		 */
		ETF_ROBUST_TX_RECORD_DESTROYER_TIMER,
		/**
		 * redis中此前缀的key充当了retry重试任务的定时器： 
		 * --为其设置过期时间，设为希望重试任务执行的时间
		 * --然后监听__keyevent@0__:expired，在redis过期清理事件中找到此前缀的key 然后为其执行重试操作
		 * --因为过期时value以找不到，因此要求key中包含重试所需的全部信息：etfEnumClass、etfTransType、bizId
		 */
		ETF_ROBUST_FAILURE_RETRY_TIMER,
		/**
		 * 前缀的key用于存储重试任务，不设过期时间，
		 * 在设置重试timer时同时保存，在timer触发重试任务执行后删除；
		 * 此类型key的作用是提高交易重试机制的可靠性： 
		 * 当重试timer因故未触发重试（例如timer到期时没有监听器在运行）而导致重试停滞， 还有机会通过一个schedule轮询此队列来补救
		 */
		ETF_ROBUST_FAILURE_RETRY_QUEUE,
		/**
		 * 跟ETF_FAILURE_RETRY_TIMER类似，在redis中此前缀的key充当了transQuery任务的定时器
		 */
		ETF_ROBUST_TRANS_QUERY_TIMER,
		/**
		 * 跟ETF_FAILURE_RETRY_QUEUE类似，用于提高交易查询机制的可靠性
		 */
		ETF_ROBUST_TRANS_QUERY_QUEUE,
		/**
		 * 达到最大重试次数 仍然失败后，trId存入此list
		 */
		ETF_ROBUST_FAILURE_RETRY_MAX_TIMES_LIST,
		/**
		 * 达到最大重试次数 仍然失败后，trId存入此list
		 */
		ETF_ROBUST_FAILURE_QUERY_MAX_TIMES_LIST;
	}

	@Override
	public EtfAbstractRedisLockTemplate getEtfConcurrentLock(String robTxEnumClazzName, String robTxEnumValueName,
			String bizId, int expireSeconds) {
		return new EtfAbstractRedisLockTemplate(redisTemplate, expireSeconds, UUID.randomUUID().toString()) {
			@Override
			protected String constructKey() {
				return calcEtfInvokeLockKey(robTxEnumClazzName, robTxEnumValueName, bizId);
			}
		};
	}

	String calcEtfInvokeLockKey(String robTxEnumClazzName, String robTxEnumValueName, String bizId) {
		return ETF_ROB_REDIS_KEYS.ETF_ROBUST_LOCKER + ":" + robTxEnumClazzName + "@" + robTxEnumValueName + "#" + bizId;
	}

	@Override
	public void validateTransDuplicate(EtfRobTxRecord tr) throws EtfRobErr4TransDuplicate {
		EtfRobTxRecord po = loadEtfTransRecord(tr);
		if (po != null) {
			throw new EtfRobErr4TransDuplicate(po);
		}
	}

	@Override
	public EtfRobTxRecord loadEtfTransRecord(String robTxEnumClazzName, String robTxEnumValueName, String bizId) {
		String key = calcEtfTransRecordKey(robTxEnumClazzName, robTxEnumValueName, bizId);

		return (EtfRobTxRecord) redisTemplate.opsForValue().get(key);
	}

	@Override
	public String saveTransRecord(EtfRobTxRecord tr) {
		String key = calcEtfTransRecordKey(tr.getTransTypeEnumClazz(), tr.getTransType(), tr.getBizId());

		redisTemplate.opsForValue().set(key, tr);

		if (tr.getTransSuccess() != null && tr.getNextQueryTime() == null && tr.getNextRetryTime() == null) {
			this.log4TxSummaryOnCompleteThenExpire(key, tr);
		}
		return key;
	}

	private void log4TxSummaryOnCompleteThenExpire(String key, EtfRobTxRecord tr) {

		String summary = "Tx[" + key + "]'s lifecycle ended with " + (tr.getTransSuccess() ? "success:)" : "failure!");

		if (tr.getRetryCount() != null) {
			summary += ("Retried " + tr.getRetryCount() + " times.");
		}

		if (tr.getQueryTransSuccess() != null) {
			summary += "And then do transQueryOrNext " + (tr.getQueryTransSuccess() ? "success:)" : "failure!");
		}

		if (etfRobBeanUtil.getEtfRobTxBackupImpl() != null) {
			etfRobBeanUtil.getEtfRobTxBackupImpl().doBackUp(tr);
			int ttlSeconds = 300;
			redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
			logger.debug("当前spring容器存在EtfRobTxBackupInterface交易备份组件，设置" + ttlSeconds + "秒过期时间");
		} else {
			logger.debug(summary + "\n>>>print tx detail before destroy:\n" + JSONObject.toJSONString(tr));
			int ttlSeconds = 3600;
			redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
			logger.debug("当前spring容器不存在EtfRobTxBackupInterface交易备份组件，设置" + ttlSeconds + "秒过期时间");
		}
	}

	protected String calcEtfTransRecordKey(String robTxEnumClazzName, String robTxEnumValueName, String bizId) {
		return ETF_ROB_REDIS_KEYS.ETF_ROBUST_TX_RECORD + ":" + robTxEnumClazzName + "@" + robTxEnumValueName + "#"
				+ bizId;
	}

	@Override
	public void updateTransRecordNextRetry(EtfRobTxRecord tr, Date nextRetryTime) {
		tr.setNextRetryTime(nextRetryTime);
		EtfRobTxRecord po = loadEtfTransRecord(tr.getTransTypeEnumClazz(), tr.getTransType(), tr.getBizId());
		po.setRetryCount(tr.getRetryCount());
		po.setNextRetryTime(nextRetryTime);

		saveTransRecord(po);
	}

	@Override
	public void updateTransRecordRetrySuccess(EtfRobTxRecord tr, String resultJson) {
		EtfRobTxRecord po = loadEtfTransRecord(tr);
		po.setTransResultJson(resultJson);
		po.setTransSuccess(true);
		po.setRetryCount(tr.getRetryCount() == null ? 1 : tr.getRetryCount());
		po.setNextRetryTime(null);
		po.setQueryCount(tr.getQueryCount());
		saveTransRecord(po);
	}

	@Override
	public void updateTransMaxRetryTimesAndInsertFailureList(EtfRobTxRecord tr) {
		EtfRobTxRecord po = loadEtfTransRecord(tr);
		po.setNextRetryTime(null);
		po.setTransSuccess(false);
		saveTransRecord(po);

		String trKey = calcEtfTransRecordKey(tr.getTransTypeEnumClazz(), tr.getTransType(), tr.getBizId());
		redisTemplate.opsForList().leftPush(ETF_ROB_REDIS_KEYS.ETF_ROBUST_FAILURE_RETRY_MAX_TIMES_LIST.toString(),
				trKey);
		logger.error(trKey + "达到最大重试次数，存入" + ETF_ROB_REDIS_KEYS.ETF_ROBUST_FAILURE_RETRY_MAX_TIMES_LIST + "等待后续处理！");
	}

	/**
	 * 事务操作 https://docs.spring.io/spring-data/redis/docs/1.7.1.RELEASE/reference/html/#tx
	 */
	@Override
	public void insertEtfRetryQueueAndTimer(EtfRobTxRecord tr) {
		String retryTime = new SimpleDateFormat("yyyyMMdd_HHmm").format(tr.getNextRetryTime());
		String key4Timer = ETF_ROB_REDIS_KEYS.ETF_ROBUST_FAILURE_RETRY_TIMER + ":" + retryTime + ":"
				+ tr.getTransTypeEnumClazz() + "@" + tr.getTransType() + "#" + tr.getBizId();
		String key4Queue = ETF_ROB_REDIS_KEYS.ETF_ROBUST_FAILURE_RETRY_QUEUE + ":" + retryTime + ":"
				+ tr.getTransTypeEnumClazz() + "@" + tr.getTransType() + "#" + tr.getBizId();
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
	public void addTrTransLog(EtfRobTxRecord tr, EtfRobTxRecordLog etfLog) {
		EtfRobTxRecord po = loadEtfTransRecord(tr);
		po.getLogList().add(etfLog);
		saveTransRecord(po);
	}

	@Override
	public void insertEtfQueryQueueAndTimer(EtfRobTxRecord tr) {
		String queryTime = new SimpleDateFormat("yyyyMMdd_HHmm").format(tr.getNextQueryTime());
		String key = ETF_ROB_REDIS_KEYS.ETF_ROBUST_TRANS_QUERY_TIMER + ":" + queryTime + ":"
				+ tr.getTransTypeEnumClazz() + "@" + tr.getTransType() + "#" + tr.getBizId();
		redisTemplate.opsForValue().set(key, "");
		redisTemplate.expireAt(key, tr.getNextQueryTime());
	}

	@Override
	public void updateTransMaxQueryTimesAndInsertFailureList(EtfRobTxRecord tr) {
		EtfRobTxRecord po = loadEtfTransRecord(tr);
		po.setNextQueryTime(null);
		po.setQueryTransSuccess(false);
		saveTransRecord(po);

		String trKey = calcEtfTransRecordKey(tr.getTransTypeEnumClazz(), tr.getTransType(), tr.getBizId());
		redisTemplate.opsForList().leftPush(ETF_ROB_REDIS_KEYS.ETF_ROBUST_FAILURE_QUERY_MAX_TIMES_LIST.toString(),
				trKey);
		logger.error(trKey + "达到最大交易查询次数，存入" + ETF_ROB_REDIS_KEYS.ETF_ROBUST_FAILURE_QUERY_MAX_TIMES_LIST + "等待后续处理！");
	}

	@Override
	public void updateTransRecordNextQuery(EtfRobTxRecord tr, Date nextQueryTime) {
		tr.setNextRetryTime(nextQueryTime);
		EtfRobTxRecord po = loadEtfTransRecord(tr.getTransTypeEnumClazz(), tr.getTransType(), tr.getBizId());
		po.setQueryCount(tr.getQueryCount());
		po.setNextQueryTime(nextQueryTime);

		saveTransRecord(po);
	}

	@Override
	public void updateTransRecordQuerySuccess(EtfRobTxRecord tr) {
		EtfRobTxRecord po = loadEtfTransRecord(tr);
		po.setQueryTransSuccess(true);
		po.setQueryCount(tr.getQueryCount() == null ? 1 : tr.getQueryCount());
		po.setNextQueryTime(null);
		saveTransRecord(po);
	}

	@Override
	public void updateTransRecordQueryFailure(EtfRobTxRecord tr) {
		EtfRobTxRecord po = loadEtfTransRecord(tr);
		po.setQueryTransSuccess(false);
		po.setQueryCount(tr.getQueryCount() == null ? 1 : tr.getQueryCount());
		po.setNextQueryTime(null);
		saveTransRecord(po);
	}

	@Override
	public void addTransDuplicateInvokeLog(EtfRobTxRecord tr) {
		EtfRobTxRecord po = loadEtfTransRecord(tr);
		EtfRobTxRecordLog etfLog = new EtfRobTxRecordLog();
		etfLog.setCrtDate(new Date());
		etfLog.setLogType(TRANS_EXE_MODE.duplicate);
		etfLog.setError(EtfRobErr4TransDuplicate.class.getName());
		po.getLogList().add(etfLog);
		saveTransRecord(po);
	}

	private EtfRobTxRecord loadEtfTransRecord(EtfRobTxRecord tr) {
		EtfRobTxRecord po = loadEtfTransRecord(tr.getTransTypeEnumClazz(), tr.getTransType(), tr.getBizId());
		return po;
	}

	public Set<String> listFailureRetryQueue() {
		return redisTemplate.keys(ETF_ROB_REDIS_KEYS.ETF_ROBUST_FAILURE_RETRY_QUEUE.name() + "*");
	}

	@Override
	public void deleteEtfRetryQueueByTimerKey(String currEtfTransRetryTimerKey) {
		String keySurfix = currEtfTransRetryTimerKey
				.substring(ETF_ROB_REDIS_KEYS.ETF_ROBUST_FAILURE_RETRY_TIMER.name().length());

		String retryQueueKey = ETF_ROB_REDIS_KEYS.ETF_ROBUST_FAILURE_RETRY_QUEUE + keySurfix;
		redisTemplate.delete(retryQueueKey);
	}

	@Override
	public void deleteEtfQueryQueueByTimerKey(String currEtfTransQueryTimerKey) {
		String keySurfix = currEtfTransQueryTimerKey
				.substring(ETF_ROB_REDIS_KEYS.ETF_ROBUST_TRANS_QUERY_TIMER.name().length());

		String queryQueueKey = ETF_ROB_REDIS_KEYS.ETF_ROBUST_TRANS_QUERY_QUEUE + keySurfix;

		redisTemplate.delete(queryQueueKey);
	}
}