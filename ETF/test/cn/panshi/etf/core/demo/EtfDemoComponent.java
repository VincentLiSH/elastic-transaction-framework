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
			protected boolean doTransQueryByEtf(String queryTimerKey, Integer queryCount)
					throws EtfException4TransQueryReturnFailureResult, EtfException4MaxQueryTimes {
				logger.debug("第" + queryCount + "次轮询交易结果" + queryTimerKey + "一次性成功");
				return true;
			}
		};
		return etfTemplate.executeEtfTransaction();
	}
}