# elastic-transaction-framework 弹性事务框架概述

## 首先上一段代码，可以最好的展示此框架如何以无侵入方式 对现有业务代码做ETF增强：

``` java
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
				;
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
