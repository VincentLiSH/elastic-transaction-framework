package cn.panshi.etf.core;

import java.util.Set;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cn.panshi.etf.core.demo.EtfDemoComponent;
import cn.panshi.etf.core.demo.EtfDemoEnum;
import cn.panshi.etf.core.demo.EtfDemoVo;
import junit.framework.Assert;

@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring-test*.xml" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EtfTemplateTest {
	Logger				logger	= Logger.getLogger(EtfDemoComponent.class);
	@Resource
	EtfDemoComponent	etfDemoComponent;
	@Resource
	EtfDaoRedis			etfDaoRedis;
	@Resource
	RedisTemplate		redisTemplate;

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
		vo.setCode("bizId");
		try {
			etfDemoComponent.doSometh_Critical_NeedFailureRetry(vo);

			Assert.fail("应该抛EtfException4TransNeedRetry");
		} catch (Exception e) {
			Thread.sleep(10 * 1000);//等待etf框架自动重试
		}

		EtfTransRecord tr = etfDaoRedis.loadEtfTransRecord(EtfDemoEnum.class.getName(),
				EtfDemoEnum.TX_need_retry.name(), vo.getCode());
		logger.debug("10秒后交易重试应该成功，交易结果:[" + tr.getTransResultJson() + "]");
		Assert.assertEquals("\"return bizId\"", tr.getTransResultJson());

		logger.debug("10秒后交易重试应该成功，交易成功flag:[" + tr.getTransSuccess() + "]");
		Assert.assertTrue(tr.getTransSuccess());
	}

	@Test
	public void testDoSometh_NeedQueryOnSuccess() throws Exception {
		String transEnumValue = EtfDemoEnum.TX_need_trans_query_on_success.name();
		String transEnumClass = EtfDemoEnum.class.getName();

		EtfDemoVo vo = new EtfDemoVo();
		vo.setCode("bizId");
		etfDemoComponent.doSometh_Critical_NeedTransQueryOnSuccess(vo);

		EtfTransRecord tr = etfDaoRedis.loadEtfTransRecord(transEnumClass, transEnumValue, vo.getCode());

		logger.debug("交易结果:[" + tr.getTransResultJson() + "]");
		Assert.assertEquals("\"return bizId\"", tr.getTransResultJson());

		logger.debug("交易getTransSuccess应该为true:[" + tr.getTransSuccess() + "]");
		Assert.assertTrue(tr.getTransSuccess());
		logger.debug("交易getLogList size应该为1:[" + tr.getLogList().size() + "]");
		Assert.assertEquals(1, tr.getLogList().size());

		logger.debug("交易getQueryTransSuccess应该为空:[" + tr.getQueryTransSuccess() + "]");
		Assert.assertNull(tr.getQueryTransSuccess());

		Thread.sleep(10 * 1000);//sleep 10秒钟 等待etf框架自动查询

		tr = etfDaoRedis.loadEtfTransRecord(transEnumClass, transEnumValue, vo.getCode());
		logger.debug("查询完成后，etf交易getQueryTransSuccess应该为trur:[" + tr.getQueryTransSuccess() + "]");
		Assert.assertTrue(tr.getQueryTransSuccess());

		logger.debug("查询完成后，etf交易getLogList size应该为2:[" + tr.getLogList().size() + "]");
		Assert.assertEquals(2, tr.getLogList().size());
	}

}