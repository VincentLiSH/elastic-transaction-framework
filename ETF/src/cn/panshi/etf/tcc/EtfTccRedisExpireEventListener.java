package cn.panshi.etf.tcc;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.alibaba.fastjson.JSONObject;

import cn.panshi.etf.tcc.EtfTccDaoRedis.ETF_TCC_KEYS;

@Configuration
public class EtfTccRedisExpireEventListener {
	static Logger logger = Logger.getLogger(EtfTccRedisExpireEventListener.class);
	@Resource
	EtfTccBeanUtil etfTccBeanUtil;
	@Resource
	EtfTccDao etfTccDao;
	@Resource
	ThreadPoolTaskExecutor executor;

	@Bean
	RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory) {

		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.addMessageListener(new RedisExpiredListener(), new PatternTopic("__keyevent@0__:expired"));
		return container;
	}

	class RedisExpiredListener implements MessageListener {

		@Override
		public void onMessage(Message message, byte[] bytes) {

			try {
				byte[] body = message.getBody();// 建议使用: valueSerializer
				byte[] channel = message.getChannel();
				String expireKey = new String(body);

				boolean matchingKey = false;
				for (ETF_TCC_KEYS key : ETF_TCC_KEYS.values()) {
					if (StringUtils.startsWith(expireKey, key.name())) {
						matchingKey = true;
					}
				}

				if (!matchingKey) {
					return;
				}

				logger.debug(String.format("redis queue: %s, body: %s", new String(channel), expireKey));

				if (StringUtils.startsWith(expireKey, ETF_TCC_KEYS.ETF_TCC_TIMER_CANCEL.name())) {
					String bizId = expireKey.substring(expireKey.indexOf("#") + 1);

					String transTypeEnumClazz = expireKey.substring(expireKey.lastIndexOf(":") + 1,
							expireKey.indexOf("#"));

					List<EtfTccRecordStep> trStepList = etfTccDao.queryTccRecordStepList(transTypeEnumClazz, bizId);
					logger.debug(
							"查找到需要cancel的TCC[" + transTypeEnumClazz + "#" + bizId + "] step" + trStepList.size() + "个");
					for (EtfTccRecordStep step : trStepList) {
						executor.submit(new Runnable() {
							@Override
							public void run() {
								EtfTccRecordStep tr = etfTccDao.loadTccTransRecordStep(transTypeEnumClazz,
										step.getTccEnumValue(), bizId);
								JSONObject paramJsonObj = JSONObject.parseObject(tr.getBizStateJson());

								EtfTccAop.setCurrTccTryStage();
								EtfTccAop.setCURR_INVOKE_BIZ_ID(bizId);
								EtfTccAop.setCURR_INVOKE_TCC_ENUM_CLAZZ_NAME(transTypeEnumClazz);
								EtfTccAop.setCURR_INVOKE_TCC_ENUM_VALUE(step.getTccEnumValue());

								EtfTccAop.setCurrTccCancelStage();
								etfTccBeanUtil.invokeEtfBean(transTypeEnumClazz, step.getTccEnumValue(), paramJsonObj);
							}
						});
					}
				} else if (StringUtils.startsWith(expireKey, ETF_TCC_KEYS.ETF_TCC_TIMER_CONFIRM.name())) {
					String bizId = expireKey.substring(expireKey.indexOf("#") + 1);

					String transTypeEnumClazz = expireKey.substring(expireKey.lastIndexOf(":") + 1,
							expireKey.indexOf("#"));

					List<EtfTccRecordStep> trStepList = etfTccDao.queryTccRecordStepList(transTypeEnumClazz, bizId);
					for (EtfTccRecordStep step : trStepList) {
						executor.submit(new Runnable() {
							@Override
							public void run() {
								EtfTccRecordStep tr = etfTccDao.loadTccTransRecordStep(transTypeEnumClazz,
										step.getTccEnumValue(), bizId);
								JSONObject paramJsonObj = JSONObject.parseObject(tr.getBizStateJson());

								EtfTccAop.setCurrTccTryStage();
								EtfTccAop.setCURR_INVOKE_BIZ_ID(bizId);
								EtfTccAop.setCURR_INVOKE_TCC_ENUM_CLAZZ_NAME(transTypeEnumClazz);
								EtfTccAop.setCURR_INVOKE_TCC_ENUM_VALUE(step.getTccEnumValue());

								EtfTccAop.setCurrTccConfirmStage();
								etfTccBeanUtil.invokeEtfBean(transTypeEnumClazz, step.getTccEnumValue(), paramJsonObj);
							}
						});
					}

				}

			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}

		}
	}
}