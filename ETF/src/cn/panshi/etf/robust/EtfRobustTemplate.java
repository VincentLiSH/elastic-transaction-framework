package cn.panshi.etf.robust;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSONObject;

import cn.panshi.etf.robust.EtfRobTxRecordLog.TRANS_EXE_MODE;

/**
 * ETF——Elastic Transaction Framework，通过（为那些无法享受ACID事务的交易）提供以下4个机制，来保证非ACID交易的可靠执行和最终一致性：
 * <li> 1 AntiDuplication幂等/防重复交易:判断同一交易同一业务流水 如果之前已经执行成功，直接返回结果</li>
 * <li> 2 LockConcurrent交易锁并发控制：同一交易（交易类型+业物流水号）不允许并发执行</li>
 * <li> 3 FailureRetry失败重试：交易执行失败时(只有在抛出EtfRetriableException情况下) 记录重试标记和重试规则（最大重试次数、重试时间间隔）；重试失败后 更新已重试次数；</li>
 * <li> 4 TransQuery交易结果查询：对于需要轮询交易结果的交易，框架会在交易完成后 主动查询交易结果（直到查询到结果并处理）；通常以梯度递增的时间间隔做查询；</li>
 * <li> 5 也许将来还可以有RollBack交易回退、Diagnose交易诊断。</li>
 * <br/>
 * EtfRobustTemplate作为ETF框架最重要的组件，封装了ETF交易处理过程最核心的抽象的实现，设计思路完全参照Spring的各种Template，实现逻辑如下：
 * <li> 1 获取当前运行模式TRANS_EXE_MODE：normal正常调用，retry框架重试调用；query框架查询交易</li>
 * <li> 2 获取业务流水号bizId：normal正常调用时 从子类实现获取bizId；其它模式时 从EtfAop的ThreadLocal获取bizId；</li>
 * <li> 3 反射获取transType：calcCurrTransType()</li>
 * <li> 4 获取交易锁防止ETF交易并发：EtfAbstractRedisLockTemplate</li>
 * <li> 5 根据mode 分别调用不同的回调：</li>
 * <li> 5.1 normal模式调用正常交易逻辑，保存ETF交易状态，并处理各种情况：失败时发起异步重试，需要交易查询的 发起异步交易查询；</li>
 * <li> 5.2 retry模式 调用重试逻辑并处理重试各种情况：仍然失败的 继续重试，超过最大重试次数的 结束交易；</li>
 * <li> 5.3 query模式调用交易查询逻辑并处理各种情况：查询无果或报错的继续查询，超过最大查询次数的 结束查询；</li>
 * <li> 6 释放交易并发锁：etfLock.unlock()；</li>
 * 
 * @author 李英权 <49069554@qq.com>
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class EtfRobustTemplate<T_etf_rob_trans_enum extends Enum<T_etf_rob_trans_enum>, T_return> {
	private static final int ETF_ROB_LOCK_DEFAULT_EXPIRE_SECONDS = 600;

	static Logger logger = Logger.getLogger(EtfRobustTemplate.class);

	/**
	 * 获取第二个泛型的class，用于处理幂等 直接返回结果   http://www.blogjava.net/calvin/archive/2009/12/10/43830.html
	 */
	Class<T_return> returnClass = (Class<T_return>) ((ParameterizedType) getClass().getGenericSuperclass())
			.getActualTypeArguments()[1];

	EtfRobDao etfRobDao;

	private T_etf_rob_trans_enum currEtfRobTxEnumValue;

	protected EtfRobustTemplate(EtfRobDao etfRobDao) {
		super();
		this.etfRobDao = etfRobDao;
	}

	/**
	 * 
	 * @return
	 * @throws EtfRobErr4InvalidTransType
	 * @throws EtfRobErr4LockConcurrent
	 */
	public final T_return executeEtfRobustTransaction() throws EtfRobErr4InvalidTransType, EtfRobErr4LockConcurrent {
		TRANS_EXE_MODE exeMode = calcTransExeMode();

		String bizId = null;
		if (exeMode == TRANS_EXE_MODE.normal) {
			bizId = calcRobustTxBizId();
		} else {
			bizId = EtfRobAop.getCurrEtfBizId();
		}

		currEtfRobTxEnumValue = calcCurrRobTxEnumValue();

		String currRobTxDisplayName = getCurrRobTxDisplayName(currEtfRobTxEnumValue, bizId);

		EtfAbstractRedisLockTemplate etfLock = etfRobDao.getEtfConcurrentLock(
				currEtfRobTxEnumValue.getClass().getName(), currEtfRobTxEnumValue.name(), bizId,
				ETF_ROB_LOCK_DEFAULT_EXPIRE_SECONDS);

		boolean lockSuccess = etfLock.lock();
		if (!lockSuccess) {
			String error = currRobTxDisplayName + " failed to get lock";
			logger.warn(error);
			throw new EtfRobErr4LockConcurrent(error);
		}
		try {
			logger.debug(currRobTxDisplayName + " started at mode " + exeMode);

			if (exeMode == TRANS_EXE_MODE.normal) {
				return this.exeNormalMode(bizId);
			} else {
				EtfRobTxRecord tr = etfRobDao.loadEtfTransRecord(currEtfRobTxEnumValue.getClass().getName(),
						currEtfRobTxEnumValue.name(), bizId);

				logger.debug(currRobTxDisplayName + " is running at " + exeMode
						+ " mode,loaded transRecord.BizStateJson:" + tr.getBizStateJson());

				if (exeMode == TRANS_EXE_MODE.retry) {
					return this.exeRetryMode(tr);
				} else if (exeMode == TRANS_EXE_MODE.after_success) {
					this.exeQueryMode(tr);
					return null;
				} else {
					throw new EtfRobErr4InvalidTransType(
							currRobTxDisplayName + " is running at invalid mode " + exeMode);
				}
			}
		} catch (Exception e) {
			logger.error(
					currRobTxDisplayName + " finished with exception " + e.getClass().getName() + ":" + e.getMessage());
			throw e;
		} finally {

			if (lockSuccess) {
				Long unlock = etfLock.unlock();
				System.out
						.println(currRobTxDisplayName + " finished " + exeMode + " then released lock[" + unlock + "]");
			}
		}
	}

	private void exeQueryMode(EtfRobTxRecord tr) {
		String currRobTxDisplayName = getCurrRobTxDisplayName(currEtfRobTxEnumValue, tr.getBizId());
		if (tr.getQueryTransSuccess() != null && tr.getQueryTransSuccess()) {
			String error = currRobTxDisplayName + " 已经查询交易结果成功，不应继续轮询交易结果！";
			logger.warn(error);
			throw new RuntimeException(error);
		}

		if (reachMaxQueryTimes(tr)) {
			String error = currRobTxDisplayName + "超过最大查询次数" + (tr.getQueryCount() - 1);
			logger.warn(error);
			etfRobDao.updateTransMaxQueryTimesAndInsertFailureList(tr);
			throw new EtfRobErr4MaxQueryTimes(error);
		}

		tr.setQueryCount(tr.getQueryCount() == null ? 1 : tr.getQueryCount() + 1);
		Exception ex = null;
		try {
			logger.info(currRobTxDisplayName + "开始第" + tr.getQueryCount() + "次查询");

			boolean querySuccess = defineBizOfQueryOrNextOnSuccess(EtfRobAop.getCurrEtfTransQueryTimerKey(),
					tr.getQueryCount());// 回调子类的交易查询逻辑

			if (querySuccess) {
				logger.info(currRobTxDisplayName + "第" + tr.getQueryCount() + "次查询成功！");
			} else {
				ex = new EtfRobErr4TransQueryNoResult();
				logger.info(currRobTxDisplayName + "第" + tr.getQueryCount() + "次查询无结果，继续。。。");
			}
		} catch (Exception e) {
			logger.error(currRobTxDisplayName + "第" + tr.getQueryCount() + "次查询失败：" + e.getMessage());
			ex = e;
		} finally {
			if (ex != null) {
				if (ex instanceof EtfRobErr4TransQueryReturnFailureResult) {
					etfRobDao.updateTransRecordQueryFailure(tr);
				} else {
					EtfRobustTx apiAnn = EtfRobAop.getCurrEtfApiAnn();
					int futureSeconds = tr.getQueryCount().intValue() == 1 ? apiAnn.queryFirstDelaySeconds()
							: apiAnn.queryIntervalSeconds();
					Date nextQueryTime = this.calcFutureTime(futureSeconds);

					etfRobDao.updateTransRecordNextQuery(tr, nextQueryTime);

					tr.setNextQueryTime(nextQueryTime);
					etfRobDao.insertEtfQueryQueueAndTimer(tr);
				}
			} else {
				etfRobDao.updateTransRecordQuerySuccess(tr);
			}

			EtfRobTxRecordLog etfLog = new EtfRobTxRecordLog();
			etfLog.setCrtDate(new Date());
			etfLog.setLogType(TRANS_EXE_MODE.after_success);
			if (ex != null) {//
				if (ex instanceof EtfRobErr4TransQueryNoResult) {
					etfLog.setError(EtfRobErr4TransQueryNoResult.class.getName());
				} else if (ex instanceof EtfRobErr4TransQueryReturnFailureResult) {
					etfLog.setError(((EtfRobErr4TransQueryReturnFailureResult) ex).getError());
				} else {
					StringWriter sw = new StringWriter();
					ex.printStackTrace(new PrintWriter(sw));
					String error = sw.toString();
					etfLog.setError(error.length() > 1000 ? error.substring(0, 999) : error);
				}
			}
			etfRobDao.addTrTransLog(tr, etfLog);

			etfRobDao.deleteEtfQueryQueueByTimerKey(EtfRobAop.getCurrEtfTransQueryTimerKey());
		}
	}

	private boolean reachMaxQueryTimes(EtfRobTxRecord tr) {
		return tr.getQueryCount() != null
				&& tr.getQueryCount().intValue() >= EtfRobAop.getCurrEtfApiAnn().queryMaxTimes();
	}

	private T_return exeRetryMode(EtfRobTxRecord tr) {
		String currRobTxDisplayName = getCurrRobTxDisplayName(currEtfRobTxEnumValue, tr.getBizId());
		if (tr.getTransSuccess() != null && tr.getTransSuccess()) {
			String error = currRobTxDisplayName + "已经执行成功，不应继续重试";
			logger.warn(error);
			return null;
		}

		if (reachMaxRetryTimes(tr)) {
			String error = currRobTxDisplayName + "超过最大重试次数" + (tr.getRetryCount() - 1);
			logger.warn(error);
			etfRobDao.updateTransMaxRetryTimesAndInsertFailureList(tr);
			return null;
		}
		tr.setRetryCount(tr.getRetryCount() == null ? 1 : tr.getRetryCount() + 1);
		Exception ex = null;
		T_return result = null;
		try {
			logger.info(currRobTxDisplayName + "开始第" + tr.getRetryCount() + "次重试");
			defineBizOfRetryOnFailure(EtfRobAop.getCurrEtfTransRetryTimerKey(), tr.getRetryCount());
			logger.info(currRobTxDisplayName + "第" + tr.getRetryCount() + "次重试成功！");

			result = constructReturnValue();

			return result;
		} catch (Exception e) {
			logger.error(currRobTxDisplayName + "第" + tr.getRetryCount() + "次重试又失败：" + e.getMessage());
			ex = e;
			throw e;
		} finally {
			if (ex != null) {
				EtfRobustTx apiAnn = EtfRobAop.getCurrEtfApiAnn();
				int futureSeconds = tr.getRetryCount().intValue() == 1 ? apiAnn.retryFirstDelaySeconds()
						: apiAnn.retryIntervalSeconds();
				Date nextRetryTime = this.calcFutureTime(futureSeconds);
				etfRobDao.updateTransRecordNextRetry(tr, nextRetryTime);
				tr.setNextRetryTime(nextRetryTime);
				etfRobDao.insertEtfRetryQueueAndTimer(tr);
			} else {
				this.try2TransQuery(tr);
				etfRobDao.updateTransRecordRetrySuccess(tr, result == null ? null : JSONObject.toJSONString(result));
			}

			EtfRobTxRecordLog etfLog = new EtfRobTxRecordLog();
			etfLog.setCrtDate(new Date());
			etfLog.setLogType(TRANS_EXE_MODE.retry);
			if (ex != null) {
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));
				String error = sw.toString();
				etfLog.setError(error.length() > 1000 ? error.substring(0, 999) : error);
			}
			etfRobDao.addTrTransLog(tr, etfLog);

			etfRobDao.deleteEtfRetryQueueByTimerKey(EtfRobAop.getCurrEtfTransRetryTimerKey());
		}

	}

	private boolean reachMaxRetryTimes(EtfRobTxRecord tr) {
		return tr.getRetryCount() != null
				&& tr.getRetryCount().intValue() >= EtfRobAop.getCurrEtfApiAnn().retryMaxTimes();
	}

	private T_return exeNormalMode(String bizId) throws EtfRobErr4TransNeedRetry {
		EtfRobTxRecord transRecord = this.constructNewEtfTransRecord(bizId);
		T_return result = null;
		Exception ex = null;
		try {
			etfRobDao.validateTransDuplicate(transRecord);// 防止交易重复执行/保证交易幂等

			defienBizOfNormal();

			result = constructReturnValue();

			return result;
		} catch (EtfRobErr4TransDuplicate e) {
			ex = e;
			logger.error(e.getMessage(), e);
			return this.returnResultOfPastInvokeDirectly(e.getEtfTransRecord().getTransResultJson());
		} catch (EtfRobErr4TransNeedRetry e) {
			ex = e;
			logger.error(e.getMessage(), e);
			throw e;
		} finally {
			this.saveNewTransRecord(transRecord, result, ex);
		}
	}

	private void saveNewTransRecord(EtfRobTxRecord tr, T_return result, Exception ex) {
		tr.setCrtDate(new Date());
		tr.setBizStateJson(EtfRobAop.getCurrEtfInvokeParam().toJSONString());
		tr.setTransSuccess(ex == null);
		if (ex == null) {
			tr.setTransResultJson(result == null ? null : JSONObject.toJSONString(result));
			tr.setTransSuccess(true);

			this.try2TransQuery(tr);
		} else if (ex instanceof EtfRobErr4TransNeedRetry) {
			tr.setNextRetryTime(this.calcFutureTime(EtfRobAop.getCurrEtfApiAnn().retryFirstDelaySeconds()));
			tr.setRetryCount(0);
			etfRobDao.insertEtfRetryQueueAndTimer(tr);
			logger.warn(getCurrRobTxDisplayName(currEtfRobTxEnumValue, tr.getBizId())
					+ "抛出EtfException4TransNeedRetry需要重试");
		} else if (ex instanceof EtfRobErr4TransDuplicate) {
			logger.warn("EtfException4TransDuplicate 直接返回"
					+ getCurrRobTxDisplayName(currEtfRobTxEnumValue, tr.getBizId()) + "之前的结果，不保存ETF交易记录 以免覆盖之前交易！ ");
			etfRobDao.addTransDuplicateInvokeLog(tr);
			return;
		}

		EtfRobTxRecordLog etfLog = new EtfRobTxRecordLog();
		etfLog.setCrtDate(new Date());
		etfLog.setLogType(TRANS_EXE_MODE.normal);

		if (ex != null) {
			StringWriter sw = new StringWriter();
			ex.printStackTrace(new PrintWriter(sw));
			String error = sw.toString();
			etfLog.setError(error.length() > 1000 ? error.substring(0, 999) : error);

		}

		tr.getLogList().add(etfLog);
		etfRobDao.saveTransRecord(tr);

	}

	/**
	 * 判断当前ETF交易 是否需要在完成后做交易查询，如果需要 则插入任务到延迟处理队列
	 */
	private void try2TransQuery(EtfRobTxRecord tr) {
		EtfRobustTx apiAnn = EtfRobAop.getCurrEtfApiAnn();
		if (apiAnn.queryMaxTimes() > 0) {
			logger.info(getCurrRobTxDisplayName(currEtfRobTxEnumValue, tr.getBizId()) + "执行完成，但需要交易查询。。。");
			int futureSeconds = (tr.getQueryCount() == null || tr.getQueryCount().intValue() == 1)
					? apiAnn.queryFirstDelaySeconds() : apiAnn.queryIntervalSeconds();
			tr.setNextQueryTime(this.calcFutureTime(futureSeconds));
			tr.setQueryCount(0);
			etfRobDao.insertEtfQueryQueueAndTimer(tr);
		}
	}

	private Date calcFutureTime(int futureTimeOfSecond) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.SECOND, futureTimeOfSecond);
		return cal.getTime();
	}

	private T_etf_rob_trans_enum calcCurrRobTxEnumValue() throws EtfRobErr4InvalidTransType {
		EtfRobustTx ann = EtfRobAop.getCurrEtfApiAnn();
		Enum[] enumConstants = ann.transEnumClazz().getEnumConstants();
		for (Enum e : enumConstants) {
			if (e.name().equals(ann.transEnumValue())) {
				return (T_etf_rob_trans_enum) e;
			}
		}
		String error = ann.transEnumValue() + " is not a valid value of enum " + ann.transEnumClazz().getName();
		logger.error(error);
		throw new EtfRobErr4InvalidTransType(error);
	}

	private String getCurrRobTxDisplayName(T_etf_rob_trans_enum type, String bizId) {
		return "ETF robust tx[" + type.getClass().getSimpleName() + "." + type.toString() + "#" + bizId + "]";
	}

	/**
	 * 为了实现幂等性，重复调用交易时，直接返回之前调用产生的结果
	 */
	private T_return returnResultOfPastInvokeDirectly(String resultJson) {
		T_return returnDirectly = resultJson == null ? null : JSONObject.parseObject(resultJson, returnClass);
		logger.debug("returnResultOfInvokBeforeDirectly:" + returnDirectly);
		return returnDirectly;
	}

	private EtfRobTxRecord constructNewEtfTransRecord(String bizId) {
		EtfRobTxRecord tr = new EtfRobTxRecord();
		tr.setBizId(bizId);
		tr.setTransType(currEtfRobTxEnumValue.name());
		tr.setTransTypeEnumClazz(currEtfRobTxEnumValue.getClass().getName());
		return tr;
	}

	/**
	 * 计算交易执行模式：正常、重试、查询
	 * ETF_BIZ_ID_ThreadLocal如果有值，说明当前调用是ETF框架timer发起的，不是正常客户端调用，是自动交易重试或交易查询
	 * @throws EtfRobErr4InvalidTransType 
	 */
	private TRANS_EXE_MODE calcTransExeMode() throws EtfRobErr4InvalidTransType {
		if (EtfRobAop.getCurrEtfBizId() == null) {
			return TRANS_EXE_MODE.normal;
		} else {
			if (EtfRobAop.getCurrEtfTransQueryTimerKey() != null) {
				return TRANS_EXE_MODE.after_success;
			} else if (EtfRobAop.getCurrEtfTransRetryTimerKey() != null) {
				return TRANS_EXE_MODE.retry;
			} else {
				EtfRobustTx currEtfApiAnn = EtfRobAop.getCurrEtfApiAnn();
				throw new EtfRobErr4InvalidTransType(
						"ETF交易[" + currEtfApiAnn.transEnumClazz().getName() + "." + currEtfApiAnn.transEnumValue() + "#"
								+ EtfRobAop.getCurrEtfBizId() + "]运行上下文不合法:不是normal/retry/query中任何一种!");
			}
		}
	}

	/**
	 * CallBack:具体子类 从交易入参获得bizId
	 */
	protected abstract String calcRobustTxBizId();

	/**
	 * CallBack:具体子类 实现真正的交易逻辑
	 */
	protected abstract void defienBizOfNormal() throws EtfRobErr4TransNeedRetry;

	/**
	 * CallBack:构造返回结果model
	 */
	protected T_return constructReturnValue() {
		logger.debug("因为交易不一定需要返回值，因此父类定义默认实现——返回空");
		return null;
	}

	/**
	 * memo：此扩展点用于实现ETF交易的后处理——只有在doBizWithinEtf成功后，ETF框架才回调此接口
	 * --典型情况是交易结果查询：在一个ETF交易完成后，不断轮询第三方查询交易结果，例如支付场景；
	 * --也可以用于将多个ETF交易串联成一个流程：在一个ETF交易完成后，调用另一个ETF交易；
	 * 必须配置@EtfAnnTransApi queryMaxTimes设置成大于0的值，否则框架不会回调此接口；
	 * 只有在doBizWithinEtf完成后（无异常，或重试成功），ETF框架才回调此接口；
	 * @return true 查询返回交易结果=success 并处理完成，无需继续查询; false 查询没有查到交易结果，需继续查询
	 * @throws EtfRobErr4TransQueryReturnFailureResult 查询返回交易结果=failure 无需继续查询
	 * @throws EtfRobErr4MaxQueryTimes
	 */
	protected boolean defineBizOfQueryOrNextOnSuccess(String queryTimerKey, Integer queryCount)
			throws EtfRobErr4TransQueryReturnFailureResult, EtfRobErr4MaxQueryTimes {
		throw new UnsupportedOperationException("子类未实现交易查询逻辑！");
	}

	/**
	 * CallBack:当ETF API配置@EtfAnnTransApi retryMaxTimes>0时，需要实现此回调
	 */
	protected void defineBizOfRetryOnFailure(String retryTimerKey, Integer retryCount) {
		throw new UnsupportedOperationException("子类未实现重试逻辑！");
	}
}