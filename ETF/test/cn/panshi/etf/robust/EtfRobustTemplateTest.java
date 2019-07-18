package cn.panshi.etf.robust;

import java.util.Set;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cn.panshi.etf.robust.EtfRobDaoRedis.ETF_ROB_REDIS_KEYS;
import cn.panshi.etf.robust.demo.EtfDemoComponent;
import cn.panshi.etf.robust.demo.EtfDemoEnum;
import cn.panshi.etf.robust.demo.EtfDemoVo;

@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring-test*.xml" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EtfRobustTemplateTest {
	Logger logger = Logger.getLogger(EtfDemoComponent.class);
	@Resource
	EtfDemoComponent etfDemoComponent;
	@Resource
	EtfRobDaoRedis etfRobDaoRedis;
	@Resource
	RedisTemplate redisTemplate;

	@Test
	public void aFirstStep2ClearRedis() {
		Set keys = redisTemplate.keys("ETF*");
		for (Object key : keys.toArray()) {
			redisTemplate.delete(key);
		}
	}

	@Test
	public void testDoSometh_Simple() throws Exception {
		EtfDemoVo vo = new EtfDemoVo();
		vo.setCode("bizId_Simple");
		etfDemoComponent.doSometh_Simple(vo);

	}

	@Test
	public void testDoSometh_NeedRetry() throws Exception {
		EtfDemoVo vo = new EtfDemoVo();
		vo.setCode("bizId-testDoSometh_NeedRetry");
		try {
			String result = etfDemoComponent.doSometh_Critical_NeedFailureRetry(vo);
			logger.error("应该抛EtfException4TransNeedRetry:" + result);
			Assert.fail("应该抛EtfException4TransNeedRetry");
		} catch (Exception e) {
			Thread.sleep(2 * 1000);

			Set<String> retryList = etfRobDaoRedis.listFailureRetryQueue();
			logger.debug("重试未执行前 队列size应该为1:[" + retryList.size() + "]");
			Assert.assertEquals(1, retryList.size());

			Thread.sleep(8 * 1000);//sleep 10秒钟 等待etf框架自动重试
		}

		EtfRobTxRecord tr = etfRobDaoRedis.loadEtfTransRecord(EtfDemoEnum.class.getName(),
				EtfDemoEnum.TX_need_retry.name(), vo.getCode());
		logger.debug("10秒后交易重试应该成功，交易结果:[" + tr.getTransResultJson() + "]");
		Assert.assertEquals("\"return bizId-testDoSometh_NeedRetry\"", tr.getTransResultJson());

		logger.debug("10秒后交易重试应该成功，交易成功flag:[" + tr.getTransSuccess() + "]");
		Assert.assertTrue(tr.getTransSuccess());

		Set<String> retryList = etfRobDaoRedis.listFailureRetryQueue();
		logger.debug("重试后 队列size应该为0:[" + retryList.size() + "]");
		Assert.assertEquals(0, retryList.size());
	}

	@Test
	public void testDoSometh_NeedQueryOnSuccess() throws Exception {
		String transEnumValue = EtfDemoEnum.TX_need_trans_query_on_success.name();
		String transEnumClass = EtfDemoEnum.class.getName();

		EtfDemoVo vo = new EtfDemoVo();
		vo.setCode("bizId");
		etfDemoComponent.doSometh_Critical_NeedTransQueryOnSuccess(vo);

		EtfRobTxRecord tr = etfRobDaoRedis.loadEtfTransRecord(transEnumClass, transEnumValue, vo.getCode());

		logger.debug("交易结果:[" + tr.getTransResultJson() + "]");
		Assert.assertEquals("\"return bizId\"", tr.getTransResultJson());

		logger.debug("交易getTransSuccess应该为true:[" + tr.getTransSuccess() + "]");
		Assert.assertTrue(tr.getTransSuccess());
		logger.debug("交易getLogList size应该为1:[" + tr.getLogList().size() + "]");
		Assert.assertEquals(1, tr.getLogList().size());

		logger.debug("交易getQueryTransSuccess应该为空:[" + tr.getQueryTransSuccess() + "]");
		Assert.assertNull(tr.getQueryTransSuccess());

		Thread.sleep(10 * 1000);//sleep 10秒钟 等待etf框架自动查询

		tr = etfRobDaoRedis.loadEtfTransRecord(transEnumClass, transEnumValue, vo.getCode());
		logger.debug("查询完成后，etf交易getQueryTransSuccess应该为trur:[" + tr.getQueryTransSuccess() + "]");
		Assert.assertTrue(tr.getQueryTransSuccess());

		logger.debug("查询完成后，etf交易getLogList size应该为2:[" + tr.getLogList().size() + "]");
		Assert.assertEquals(2, tr.getLogList().size());
	}

	@Test
	public void testNestedEtfInvoke() throws Exception {
		String transEnumValue = EtfDemoEnum.AndThen_Invoke_Another_ETF.name();
		String transEnumClass = EtfDemoEnum.class.getName();

		EtfDemoVo vo = new EtfDemoVo();
		vo.setCode("bizId");
		try {
			etfDemoComponent.doSometh_AndThen_Invoke_Another_ETF(vo);
		} catch (Exception e) {
			logger.debug(e.getMessage());
		}

		Thread.sleep(10 * 1000);//sleep 10秒钟 等待etf框架自动回调next

		EtfRobTxRecord tr1 = etfRobDaoRedis.loadEtfTransRecord(transEnumClass, transEnumValue, vo.getCode());

		logger.debug("父交易getTransSuccess应该为true:[" + tr1.getTransSuccess() + "]");
		Assert.assertTrue(tr1.getTransSuccess());

		Thread.sleep(20 * 1000);

		EtfRobTxRecord trNested = etfRobDaoRedis.loadEtfTransRecord(transEnumClass, EtfDemoEnum.TX_simple_Nested.name(),
				vo.getCode());

		logger.debug("子交易getTransSuccess应该为true:[" + trNested.getTransSuccess() + "]");
		Assert.assertTrue(trNested.getTransSuccess());
		logger.debug("子交易getRetryCount应该为2:[" + trNested.getRetryCount() + "]");
		Assert.assertEquals(2, trNested.getRetryCount().intValue());
	}

	@Test
	public void testReachMaxRetryTimes() throws Exception {
		String transEnumValue = EtfDemoEnum.TX_ExceedMaxRetryTimes.name();
		String transEnumClass = EtfDemoEnum.class.getName();

		EtfDemoVo vo = new EtfDemoVo();
		vo.setCode("bizId");
		try {
			etfDemoComponent.doSometh_Critical_ExceedMaxRetryTimes(vo);
		} catch (Exception e) {
			logger.debug(e.getMessage());
		}

		Thread.sleep(20 * 1000);//sleep 10秒钟 等待etf重试4次 每次间隔2秒（redis误差一般2~3秒）

		EtfRobTxRecord tr1 = etfRobDaoRedis.loadEtfTransRecord(transEnumClass, transEnumValue, vo.getCode());

		Assert.assertFalse(tr1.getTransSuccess());

		Assert.assertEquals(4, tr1.getRetryCount().intValue());

		String trKey = (String) redisTemplate.opsForList()
				.index(ETF_ROB_REDIS_KEYS.ETF_ROBUST_FAILURE_RETRY_MAX_TIMES_LIST.name(), 0);
		Assert.assertEquals(
				etfRobDaoRedis.calcEtfTransRecordKey(tr1.transTypeEnumClazz, tr1.getTransType(), tr1.getBizId()), trKey);

	}
}