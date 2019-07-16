# ETF:Elastic Transaction Framework
## 弹性事务概述
ACID事务虽好，但局限性也是很明显的——单系统本地事务处理，尤其是现在这个微服务和互联网开放架构的时代已经远远无法满足要求。

你不可能要求外部第三方的系统跟你绑定在同一个ACID事务上下文，更何况很多时候你集成的服务或API本身就是非事务性的，HTTP也不支持事务上下文传播；

你也很难要求组织内的另一个团队的系统跟你的系统纳入同一个ACID事务上下文，为了灵活性、扩展性、性能等很多架构考量都不能那么做。

就像物理世界静止是相对的运动是绝对的，软件世界的现实是：单系统ACID的简单幸福是相对的、一时的，分布式的复杂和痛苦才是永恒的真相！

IT大厂是最早感受分布式+高并发痛苦的，也就是著名的CAP难题，它们也早就指出了解决之道——一种折中的方案——最终一致性，
理论的就不展开介绍，可以搜索关键字CAP、BASE、TCC、幂等。

归纳起来结论就是：
* ACID刚性事务存在很多局限性，不适合互联网时代的分布式计算，
* 必须设计一种柔性/弹性的事务机制，所谓弹性就是不追求实时严格的事务一致性，只需保证最终一致性；

## ETF弹性事务框架介绍
本项目的目标是为那些无法享受acid事务的java分布式系统和微服务的开发，提供一种简单有效的交易最终一致性解决方案。

虽然优秀的程序员不用任何ETF之类的框架也能够手打出一个可靠的复杂交易系统，但是框架可以显著提高工作效率、架构质量和稳定性；

ETF是一个JAVA弹性事务开发框架，可以帮助程序员更加优雅、简单的处理交易的幂等、防重复、失败重试、成功后的交易结果查询、失败回退、交易日志。。。

以上这些其实都是非功能性需求，如果没有框架的帮助，这些非功能性的关注点很可能会跟真正的业务逻辑掺杂在一起，
这是很多系统维护成本高企、质量糟糕的根源。

ETF相对于其它同类项目的特点/优势：
* 使用方式比较简单优雅，框架侵入性低；annotation声明式的使用风格，对API参数返回值无限制，参见后面的代码示例；
* 基于Redis数据库，性能好，学习成本低；
* 提供Template模板辅助类，可以显著改善代码风格和质量；熟悉Spring的程序员会非常喜欢这种编程模型；
* 详细记录问题交易的执行过程，便于排查问题、恢复故障；
* 提供交易管理控制台UI，方便监控；

套用一句很多framework常用的宣传语——ETF可以让开发人员把更多精力用在真正的业务开发上。

## ETF关键设计考量

将需要弹性事务增强的交易

* 
上一段代码，展示此框架如何以无侵入方式对现有业务代码做ETF增强：
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
