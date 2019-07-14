package cn.panshi.etf.core;

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

import com.alibaba.fastjson.JSONObject;

import cn.panshi.etf.core.EtfDaoRedis.ETF_REDIS_KEYS;

@Configuration
public class EtfRedisExpireEventListener {
	static Logger logger = Logger.getLogger(EtfRedisExpireEventListener.class);

	@Resource
	EtfTransBeanUtil etfTransBeanUtil;
	@Resource
	EtfDao etfDao;

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
				for (ETF_REDIS_KEYS key : ETF_REDIS_KEYS.values()) {
					if (StringUtils.startsWith(expireKey, key.name())) {
						matchingKey = true;
					}
				}

				if (!matchingKey) {
					return;
				}

				logger.debug(String.format("redis queue: %s, body: %s", new String(channel), expireKey));

				String bizId = expireKey.substring(expireKey.indexOf("#") + 1);

				String transTypeEnumClazz = expireKey.substring(expireKey.lastIndexOf(":") + 1, expireKey.indexOf("@"));

				String transType = expireKey.substring(expireKey.indexOf("@") + 1, expireKey.indexOf("#"));

				EtfTransRecord tr = etfDao.loadEtfTransRecord(transTypeEnumClazz, transType, bizId);
				JSONObject paramJsonObj = JSONObject.parseObject(tr.getBizStateJson());
				EtfAop.setCurrEtfBizId(bizId);

				if (StringUtils.startsWith(expireKey, ETF_REDIS_KEYS.ETF_FAILURE_RETRY_TIMER.name())) {
					EtfAop.setCurrEtfTransRetryTimerKey(expireKey);
					etfTransBeanUtil.invokeEtfBean(transTypeEnumClazz, transType, paramJsonObj);
				} else if (StringUtils.startsWith(expireKey, ETF_REDIS_KEYS.ETF_TRANS_QUERY_TIMER.name())) {
					EtfAop.setCurrEtfTransQueryTimerKey(expireKey);
					etfTransBeanUtil.invokeEtfBean(transTypeEnumClazz, transType, paramJsonObj);
				} else {

				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}

		}
	}
}