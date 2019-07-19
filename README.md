# ETF:Elastic Transaction Framework
## 弹性事务概述
ACID事务虽好，但局限性也是很明显的——仅适于单体系统的本地事务处理，

分布式的ACID事务不仅性能差、无法scale，而且使用限制很大（对中间件和数据库XA的要求），即便在企业应用领域也越来越被遗弃，

在微服务和互联网开放架构时代已经远远无法满足要求。

你不可能要求外部第三方的系统跟你绑定在同一个ACID事务上下文，更何况很多时候你集成的服务或API本身就是非事务性的，HTTP也不支持事务上下文传播；

你也很难要求组织内的另一个团队的系统跟你的系统纳入同一个ACID事务上下文，为了灵活性、扩展性、性能等很多架构考量都不能那么做。

就像物理世界静止是相对的运动是绝对的，软件世界的现实是：单系统ACID的简单幸福是相对的、一时的，分布式的复杂和痛苦才是永恒的真相！

事实上，分布式事务问题已经成为微服务落地最大的阻碍，也是最具挑战性的一个技术难点。

IT大厂是最早感受分布式+高并发痛苦的，即著名的CAP难题，它们也早就指出了解决之道——一种折中的方案——最终一致性，
理论的就不展开介绍，可以搜索关键字CAP、BASE、TCC、幂等。

归纳起来结论就是：
* ACID刚性事务存在很多局限性，不适合互联网时代的分布式计算和微服务架构，
* 必须设计一种柔性/弹性的事务机制，所谓弹性就是不追求实时严格的事务一致性，以空间换时间策略保证最终一致性；

## ETF弹性事务框架介绍
本项目的目标是为那些无法享受acid事务的java分布式系统和微服务的开发，提供一种简单有效的交易最终一致性解决方案。

虽然优秀的程序员不用任何ETF之类的框架也能够手打出一个可靠的复杂交易系统，但是框架可以显著提高工作效率、架构质量和稳定性；

ETF是一个JAVA弹性事务开发框架，可以帮助程序员更加优雅、简单的处理交易的幂等、防重复、失败重试、成功后的交易结果查询、交易日志。。。

以上这些其实都是非功能性需求，如果没有框架的帮助，这些非功能性的关注点很可能会跟真正的业务逻辑掺杂在一起，
这是很多系统维护成本高企、质量糟糕的根源。

ETF相对于其它同类项目的特点/优势：
* 针对可撤销和不可撤销的交易，提供两种处理模式：用TCC模式处理可撤销的交易；用失败重试模式处理不可撤销复交易；后面详细解释；
* 使用方式比较简单优雅，框架侵入性低；annotation声明式的使用风格，对API参数返回值无限制，见后面的代码示例；
* 基于Redis数据库记录问题交易日志、协调ETF事务上下文，性能好，学习成本低；
* 提供Template模板辅助类，可以显著改善代码风格和质量；熟悉Spring的程序员会非常喜欢这种编程模型；
* 详细记录问题交易的执行过程，便于排查问题、恢复故障；
* 提供交易管理控制台UI，方便监控；

套用一句很多framework常用的宣传语——ETF可以让开发人员把更多精力用在真正的业务逻辑开发上。

## ETF两种交易类型
虽然总是在说交易、交易系统，但是其实交易也分不同类型的；

我将复杂交易系统中的交易分成两类：可撤销的交易和不可撤销的交易；

判断交易类型的依据：
* 凡是不在你的架构权限管辖内的系统的接口（包括组织内和外部第三方），默认都看作不可撤销的交易；
因为这样的接口一旦调用了其中的写操作，通常是没有办法通过调用另外一个操作直接撤销的；只能走另外的流程（例如退款、退货）做反向处理；
* 可撤销型交易需要专门设计，这就涉及到了TCC模型；简单说TCC就是把交易设计成分阶段执行：try阶段锁定资源，confirm阶段执行交易，cancel撤销交易；
* 两种交易类型的本质区别就是：前者无法在出错时立即撤销，必须走另外流程（技术或业务功能）撤销；而后者在交易模型设计上就预留了撤销操作接口，可以出错后直接撤销，无需另外流程；

## 两种交易的应用场景和处理模式
不可撤销交易的应用场景比较多，基本上与外部接口打交道的都是，典型的应用就是系统对接第三方支付通道，通常采用的交易保证机制是：
* 支付平台回调/通知结果；通知无响应时会以阶梯时间段不断重试，直到有应答；
* 本系统主动轮询支付结果；
* 每日对账；

典型支付场景的设计策略是：即便出现延迟或错误，也要尽量让交易成功，因为恢复/回退交易的成本太高，会影响用户体验（重新支付引起用户疑虑）。

其实这种策略是很务实也很有效的，因为运行时的报错延迟主要都是技术原因，很多情况下也都是短暂的，完全可以也应该通过重试机制来促成交易最终的完成，
大多数情况下回退/撤销不是一个明智的做法。

对于可撤销交易，就可以应用TCC模式同步交易的各个环节分别做try操作，然后同步各环节做confirm或cancel；
TCC可撤销交易仅适用于组织内部统一架构实施，对于外部系统则无能为力。不过TCC一旦做成了也会具有更高的系统可靠性。

归纳起来就是：
* 对不可撤销交易，出错时重试，延迟响应时轮询结果；
* 对于可撤销交易，出错时撤销；

## ETF针对两种交易的支持——RobustTx和TccTx
通过前面对两种交易类型的比较发现，不可撤销型交易能够容忍暂时的错误和一定程度的延迟响应——比较健壮，

于是为了概念更加清晰准确，ETF框架为两种交易类型取名为RobustTx和TccTx，以便在框架实现和使用中都可以方便的加以区分。

基于ETF框架开发具有最终一致性保证的微服务，只需以下几个步骤：
* 基于对需求的分析和抽象，设计出一组交易；
* 通过enum枚举类型定义和声明这些交易；简单的可以一个enum，复杂的可以为每个流程定义一个enum；enum的每个值对应一个交易；
* 此时其实就需要想清楚交易的类型了：可撤销 or 不可撤销；Robust还是Tcc？
* 为每个交易定义一个Tx组件：
* --不可撤销型交易：在API标记@EtfRobustTx、配置交易enum和重试规则等；基于模板工具类EtfRobustTemplateRedis填充交易的正常业务逻辑和重试、查询、回调逻辑；
* --可撤销交易：在API标记@EtfTccTx并配置交易enum；基于模板工具类TccTemplate实现try、confirm、cancel逻辑；
更多细节可参照ETF中的junit测试代码，后续也会提供开发指南文档。

## ETF框架的低侵入性编程模型
好的开发框架应该同时具备“简单明确的契约和编程模型”和“尽量低的侵入性、对业务开发尽量少的干扰”两个特征。
* ETF要求用enum枚举类定义交易类型，这虽然是一个限制，但其实也是一个最佳实践。交易类型是如此的重要，完全配得上用一个枚举类型进行明确的声明和定义。
* ETF框架通过annotation配置TCC规则和重试/查询回调规则，对业务组件的入参和返回值完全不做限制；
* 正是由于使用了enum定义交易类型，ETF组件的配置具有了类型安全，框架在运行时也能发现配置不一致问题，尽早暴露很多隐藏的错误；这就是简单明确的编程模型的体现，让使用者不容易犯错。
* 业务开发人员只需把业务逻辑按照Etf模板类的规范分解填充到各个回调接口中，即可获得TCC和交易重试和交易查询回调。

贴一段代码展示ETF如何低侵入性的对业务代码做弹性事务增强，

不难看出，这是一个“不可恢复交易”型组件，ETF为其提供了retry和query机制确保交易（在暂时出错的情况下也能尽量）执行成功。

``` java
public enum EtfDemoEnum {
      TX_simple, TX_need_retry, TX_need_trans_query_on_success, AndThen_Invoke_Another_ETF, TX_simple_Nested;
}

@EtfRobustTx(transEnumClazz = EtfDemoEnum.class, transEnumValue = "AndThen_Invoke_Another_ETF", //           
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
```
再贴一段“TCC可恢复型交易”组件示例代码：
``` java
public enum TccDemoEnum {
	step1, step2;
}

@Resource                                                                                    
TccDemoTransComponent tccDemoTransComponent;                                                 
                                                                                             
@Resource                                                                                    
EtfTccDaoRedis etfTccDaoRedis;                                                               
                                                                                             
public void startTccFlow1() throws EtfTccException4PrepareStage, EtfTccException4StartStage {
	TccDemoVo vo = new TccDemoVo();                                                          
	vo.setCode("unit test");                                                                 
                                                                                             
	TccTransStarter<TccDemoEnum> starter = new TccTransStarter<TccDemoEnum>(etfTccDaoRedis); 
                                                                                             
	starter.prepareTccTrans(new TccTransPrepareStatement() {                                 
		@Override                                                                            
		public void doPrepare() {                                                            
			tccDemoTransComponent.tccStep1(vo);                                              
		}                                                                                    
	});                                                                                      
                                                                                             
	starter.prepareTccTrans(new TccTransPrepareStatement() {                                 
		@Override                                                                            
		public void doPrepare() {                                                            
			tccDemoTransComponent.tccStep2(vo);                                              
		}                                                                                    
	});                                                                                      
                                                                                             
	starter.startTccTransList();                                                             
}                                                                                            
```

``` java
@Resource                                                               
EtfTccDaoRedis etfTccDaoRedis;                                          
                                                                        
@EtfTcc(transEnumClazz = TccDemoEnum.class, transEnumValue = "step1")   
public void tccStep1(TccDemoVo vo) {                                    
	try {                                                               
		new EtfTccTransTemplate<TccDemoEnum>(etfTccDaoRedis) {          
                                                                        
			@Override                                                   
			protected String calcTccTransBizId() {                      
				return vo.getCode();                                    
			}                                                           
                                                                        
			@Override                                                   
			protected void tccTry() {                                   
				logger.debug("step1 try..." + vo.getCode());      
				throw new RuntimeException("step1 try 失败");             
			}                                                           
                                                                        
			@Override                                                   
			protected void tccConfirm() {                               
				logger.debug("confirm1..." + vo.getCode());       
			}                                                           
                                                                        
			@Override                                                   
			protected void tccCancel() {                                
				logger.debug("cancel1..." + vo.getCode());        
			}                                                           
		}.executeEtfTcc();                                              
	} catch (EtfException4LockConcurrent e) {                           
		e.printStackTrace();                                            
	}                                                                   
}                                                                       
                                                                        
@EtfTcc(transEnumClazz = TccDemoEnum.class, transEnumValue = "step2")   
public void tccStep2(TccDemoVo vo) {                                    
	try {                                                               
		new EtfTccTransTemplate<TccDemoEnum>(etfTccDaoRedis) {          
                                                                        
			@Override                                                   
			protected String calcTccTransBizId() {                      
				return vo.getCode();                                    
			}                                                           
                                                                        
			@Override                                                   
			protected void tccTry() {                                   
				logger.debug("try2..." + vo.getCode());           
			}                                                           
                                                                        
			@Override                                                   
			protected void tccConfirm() {                               
				logger.debug("confirm2..." + vo.getCode());       
			}                                                           
                                                                        
			@Override                                                   
			protected void tccCancel() {                                
				logger.debug("cancel2..." + vo.getCode());        
			}                                                           
		}.executeEtfTcc();                                              
	} catch (EtfException4LockConcurrent e) {                           
		e.printStackTrace();                                            
	}                                                                   
}                                                                       
``` 

作为对比可以看下另一个星数很高的tcc项目，对业务组件的侵入性是什么样的:
``` java
@Compensable(confirmMethod = "confirmRecord", cancelMethod = "cancelRecord", transactionContextEditor = MethodTransactionContextEditor.class)
public String record(TransactionContext transactionContext, CapitalTradeOrderDto tradeOrderDto) ...

public void confirmRecord(TransactionContext transactionContext, CapitalTradeOrderDto tradeOrderDto)...
public void cancelRecord(TransactionContext transactionContext, CapitalTradeOrderDto tradeOrderDto)...

```
https://github.com/changmingxie/tcc-transaction/wiki/%E4%BD%BF%E7%94%A8%E6%8C%87%E5%8D%971.2.x#%E5%9C%A8tcc-transaction-http-capital%E4%B8%AD%E5%8F%91%E5%B8%83tcc%E6%9C%8D%E5%8A%A1%E7%A4%BA%E4%BE%8B

明确要求try方法、confirm方法和cancel方法入参类型须一样 https://github.com/changmingxie/tcc-transaction/wiki/%E4%BD%BF%E7%94%A8%E6%8C%87%E5%8D%971.2.x#%E8%B0%83%E7%94%A8%E8%BF%9C%E7%A8%8Btcc%E6%9C%8D%E5%8A%A1-1

## ETF主要借助Redis的一些关键特性 实现了多种交易最终一致性机制
ETF的最关键特性，目前都是严重依赖Redis的一些特性实现的：
* 交易日志

* 交易并发控制

* 交易幂等性/交易防重

* 交易延迟重试

* 交易延迟查询处理

* TCC同步

## Quick Start
* 添加项目依赖
* 参照项目提供的最小spring配置，在项目中配置ETF依赖的RedisTemplate和threadpool，并在spring加载期扫描ETF框架所在的package；
* 定义交易类型enum
* 确定交易类型：可撤销 or 不可撤销
* 参照示例代码开发和配置ETF组件
* 参照示例，编写junit测试；
* 参照ETF交易控制台运行说明，启动控制台服务，查看测试结果；或者直接在redis数据库查看结果；
