# ETF:Elastic Transaction Framework
## 弹性事务概述
ACID事务虽好，但很多情况下也是用不上的，尤其是现在这个微服务和互联网开放架构的时代，
你不可能要求外部第三方的系统跟你绑定在同一个ACID事务上下文，更何况很多时候你集成的服务或API本身就是非事务性的；
你也很难要求组织内的另一个团队的系统跟你的系统纳入同一个ACID事务上下文，为了灵活性、扩展性、性能等很多架构考量，都不能那么做。

就像物理世界静止是相对的运动是绝对的，软件世界的现实是：单系统ACID的简单幸福是相对的、一时的，分布式的复杂和痛苦才是永恒的真相！

IT大厂是最早感受分布式+高并发痛苦的——著名的CAP难题，它们也早就指出了解决之道——一种折中的方案——最终一致性，
理论的就不展开介绍，可以搜索关键字CAP、BASE、TCC、幂等。

归纳起来结论就是：
* ACID刚性事务存在很多局限性，不适合互联网时代的分布式计算，
* 必须设计一种柔性/弹性的事务机制，所谓弹性就是不追求实时严格的事务一致性，只需保证最终一致性；

## 上一段代码，展示此框架如何以无侵入方式对现有业务代码做ETF增强：
* 对业务组件API的方法签名完全不做限制，只需添加@EtfAnnTransApi 配置重试和查询回调规则
* 只需把业务逻辑按照EtfTemplateWithRedisDao的规范分解和填充到各个回调中，即可获得交易重试和交易查询回调。

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
