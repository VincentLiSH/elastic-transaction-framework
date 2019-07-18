package cn.panshi.etf.core.demo;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import cn.panshi.etf.core.EtfAnnTransApi;
import cn.panshi.etf.core.EtfDaoRedis;
import cn.panshi.etf.core.EtfException4MaxQueryTimes;
import cn.panshi.etf.core.EtfException4TransNeedRetry;
import cn.panshi.etf.core.EtfException4TransQueryReturnFailureResult;
import cn.panshi.etf.core.EtfTemplateWithRedisDao;

@Component
public class EtfDemoComponent {
	Logger logger = Logger.getLogger(EtfDemoComponent.class);
	@Resource
	EtfDaoRedis etfDaoRedis;
	@Resource
	EtfDemoComponent2 etfDemoComponent2;

	@EtfAnnTransApi(transEnumClazz = EtfDemoEnum.class, transEnumValue = "TX_simple")
	public String doSometh_Simple(EtfDemoVo etfDemoVo) throws Exception {

		EtfTemplateWithRedisDao<EtfDemoEnum, String> etfTemplate = new EtfTemplateWithRedisDao<EtfDemoEnum, String>(
				etfDaoRedis) {

			@Override
			protected String calcEtfBizId() {
				return etfDemoVo.getCode();
			}

			@Override
			protected void doBizWithinEtf() throws EtfException4TransNeedRetry {
				logger.debug("这是个简单交易，无需失败后重试 或 成功后结果查询，使用etf仅仅为了自动记录交易日志:" + etfDemoVo.getCode());
			}

			@Override
			protected String constructResult() {
				return "return " + etfDemoVo.getCode();
			}

		};

		return etfTemplate.executeEtfTransaction();
	}

	@EtfAnnTransApi(transEnumClazz = EtfDemoEnum.class, transEnumValue = "TX_need_retry", //
			retryMaxTimes = 3, retryFirstDelaySeconds = 5, retryIntervalSeconds = 20)
	public String doSometh_Critical_NeedFailureRetry(EtfDemoVo etfDemoVo) throws Exception {

		EtfTemplateWithRedisDao<EtfDemoEnum, String> etfTemplate = new EtfTemplateWithRedisDao<EtfDemoEnum, String>(
				etfDaoRedis) {

			@Override
			protected String calcEtfBizId() {
				return etfDemoVo.getCode();
			}

			@Override
			protected void doBizWithinEtf() throws EtfException4TransNeedRetry {
				logger.debug("交易失败 需要重试:" + etfDemoVo.getCode());

				throw new EtfException4TransNeedRetry("交易失败 需要重试");
			}

			@Override
			protected void doRetryByEtf(String retryTimerKey, Integer retryCount) {
				logger.debug("第" + retryCount + "次重试" + retryTimerKey + "一次性成功");
				// throw new RuntimeException("第" + retryCount + "次重试" + retryTimerKey + "重试仍然失败");
			}

			@Override
			protected String constructResult() {
				return "return " + etfDemoVo.getCode();
			}

		};
		return etfTemplate.executeEtfTransaction();
	}

	@EtfAnnTransApi(transEnumClazz = EtfDemoEnum.class, transEnumValue = "TX_need_trans_query_on_success", //
			queryMaxTimes = 5, queryFirstDelaySeconds = 5, queryIntervalSeconds = 60)
	public String doSometh_Critical_NeedTransQueryOnSuccess(EtfDemoVo etfDemoVo) throws Exception {

		EtfTemplateWithRedisDao<EtfDemoEnum, String> etfTemplate = new EtfTemplateWithRedisDao<EtfDemoEnum, String>(
				etfDaoRedis) {

			@Override
			protected String calcEtfBizId() {
				return etfDemoVo.getCode();
			}

			@Override
			protected void doBizWithinEtf() throws EtfException4TransNeedRetry {
				logger.debug("交易完成，需要轮询交易结果:" + etfDemoVo.getCode());
			}

			@Override
			protected String constructResult() {
				return "return " + etfDemoVo.getCode();
			}

			@Override
			protected boolean doTransQueryOrNextTransByEtf(String queryTimerKey, Integer queryCount)
					throws EtfException4TransQueryReturnFailureResult, EtfException4MaxQueryTimes {
				logger.debug("第" + queryCount + "次轮询交易结果" + queryTimerKey + "一次性成功");
				return true;
			}
		};
		return etfTemplate.executeEtfTransaction();
	}

	@EtfAnnTransApi(transEnumClazz = EtfDemoEnum.class, transEnumValue = "AndThen_Invoke_Another_ETF", //
			queryMaxTimes = 5, queryFirstDelaySeconds = 8, queryIntervalSeconds = 60, //
			retryMaxTimes = 3, retryFirstDelaySeconds = 3, retryIntervalSeconds = 5)
	public String doSometh_AndThen_Invoke_Another_ETF(EtfDemoVo etfDemoVo) throws Exception {

		EtfTemplateWithRedisDao<EtfDemoEnum, String> etfTemplate = new EtfTemplateWithRedisDao<EtfDemoEnum, String>(
				etfDaoRedis) {

			@Override
			protected String calcEtfBizId() {
				return etfDemoVo.getCode();
			}

			@Override
			protected void doBizWithinEtf() throws EtfException4TransNeedRetry {
				throw new EtfException4TransNeedRetry("失败 需要重试一次");
			}

			@Override
			protected void doRetryByEtf(String retryTimerKey, Integer retryCount) {
				logger.debug("一次重试完成，需要轮询交易结果:" + etfDemoVo.getCode());
			}

			@Override
			protected String constructResult() {
				return "return " + etfDemoVo.getCode();
			}

			@Override
			protected boolean doTransQueryOrNextTransByEtf(String queryTimerKey, Integer queryCount)
					throws EtfException4TransQueryReturnFailureResult, EtfException4MaxQueryTimes {
				logger.debug("第" + queryCount + "次轮询交易结果" + queryTimerKey + "一次性成功");
				try {
					EtfDemoVo2 etfDemoVo2 = new EtfDemoVo2();
					etfDemoVo2.setCode(etfDemoVo.getCode());
					etfDemoComponent2.doSometh_Simple_By_Another_Etf(etfDemoVo2);
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
				return true;
			}
		};
		return etfTemplate.executeEtfTransaction();
	}

	@EtfAnnTransApi(transEnumClazz = EtfDemoEnum.class, transEnumValue = "TX_ExceedMaxRetryTimes", //
			retryMaxTimes = 4, retryFirstDelaySeconds = 2, retryIntervalSeconds = 2)
	public String doSometh_Critical_ExceedMaxRetryTimes(EtfDemoVo etfDemoVo) throws Exception {

		EtfTemplateWithRedisDao<EtfDemoEnum, String> etfTemplate = new EtfTemplateWithRedisDao<EtfDemoEnum, String>(
				etfDaoRedis) {

			@Override
			protected String calcEtfBizId() {
				return etfDemoVo.getCode();
			}

			@Override
			protected void doBizWithinEtf() throws EtfException4TransNeedRetry {
				logger.debug("交易失败 需要重试:" + etfDemoVo.getCode());

				throw new EtfException4TransNeedRetry("交易失败 需要重试");
			}

			@Override
			protected void doRetryByEtf(String retryTimerKey, Integer retryCount) {
				throw new RuntimeException("第" + retryCount + "次重试" + retryTimerKey + "仍然失败");
			}

			@Override
			protected String constructResult() {
				return "return " + etfDemoVo.getCode();
			}

		};
		return etfTemplate.executeEtfTransaction();
	}
}