package cn.panshi.etf.robust.demo;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import cn.panshi.etf.robust.EtfRobustTx;
import cn.panshi.etf.robust.EtfRobDaoRedis;
import cn.panshi.etf.robust.EtfRobErr4TransNeedRetry;
import cn.panshi.etf.robust.EtfRobustTemplateRedis;

@Component
public class EtfDemoComponent2 {
	Logger logger = Logger.getLogger(EtfDemoComponent2.class);
	@Resource
	EtfRobDaoRedis etfRobDaoRedis;

	@EtfRobustTx(transEnumClazz = EtfDemoEnum.class, transEnumValue = "TX_simple_Nested", //
			retryMaxTimes = 3, retryFirstDelaySeconds = 5, retryIntervalSeconds = 10)
	public String doSometh_Simple_By_Another_Etf(EtfDemoVo2 etfDemoVo) throws Exception {

		EtfRobustTemplateRedis<EtfDemoEnum, String> etfTemplate = new EtfRobustTemplateRedis<EtfDemoEnum, String>(
				etfRobDaoRedis) {

			@Override
			protected String calcRobustTxBizId() {
				return etfDemoVo.getCode();
			}

			@Override
			protected void defienBizOfNormal() throws EtfRobErr4TransNeedRetry {
				logger.debug("这是个被另一ETF组件调用的ETF交易（需要重试2次）:" + etfDemoVo.getCode());
				throw new EtfRobErr4TransNeedRetry("test 重试");
			}

			@Override
			protected String constructReturnValue() {
				return "return " + etfDemoVo.getCode();
			}

			@Override
			protected void defineBizOfRetryOnFailure(String retryTimerKey, Integer retryCount) {
				if (retryCount.intValue() == 1) {
					throw new RuntimeException("第一次重试 故意失败");
				} else {
					logger.debug("第二次重试成功！");
				}
			}
		};

		return etfTemplate.executeEtfRobustTransaction();
	}
}