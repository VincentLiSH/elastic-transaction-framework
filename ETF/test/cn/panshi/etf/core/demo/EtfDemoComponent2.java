package cn.panshi.etf.core.demo;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import cn.panshi.etf.core.EtfAnnTransApi;
import cn.panshi.etf.core.EtfDaoRedis;
import cn.panshi.etf.core.EtfException4TransNeedRetry;
import cn.panshi.etf.core.EtfTemplateWithRedisDao;

@Component
public class EtfDemoComponent2 {
	Logger logger = Logger.getLogger(EtfDemoComponent2.class);
	@Resource
	EtfDaoRedis etfDaoRedis;

	@EtfAnnTransApi(transEnumClazz = EtfDemoEnum.class, transEnumValue = "TX_simple_Nested", //
			retryMaxTimes = 5, retryFirstDelaySeconds = 6, retryIntervalSeconds = 10)
	public String doSometh_Simple_By_Another_Etf(EtfDemoVo2 etfDemoVo) throws Exception {

		EtfTemplateWithRedisDao<EtfDemoEnum, String> etfTemplate = new EtfTemplateWithRedisDao<EtfDemoEnum, String>(
				etfDaoRedis) {

			@Override
			protected String calcEtfBizId() {
				return etfDemoVo.getCode();
			}

			@Override
			protected void doBizWithinEtf() throws EtfException4TransNeedRetry {
				logger.debug("这是个被另一ETF组件调用的ETF交易（需要重试2次）:" + etfDemoVo.getCode());
				throw new EtfException4TransNeedRetry("test 重试");
			}

			@Override
			protected String constructResult() {
				return "return " + etfDemoVo.getCode();
			}

			@Override
			protected void doRetryByEtf(String retryTimerKey, Integer retryCount) {
				if (retryCount.intValue() == 1) {
					throw new RuntimeException("第一次重试 故意失败");
				} else {
					logger.debug("第二次重试成功！");
				}
			}
		};

		return etfTemplate.executeEtfTransaction();
	}
}