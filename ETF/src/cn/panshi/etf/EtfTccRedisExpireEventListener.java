package cn.panshi.etf;

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

import cn.panshi.etf.robust.EtfRobBeanUtil;
import cn.panshi.etf.robust.EtfRobDaoRedis.ETF_REDIS_KEYS;
import cn.panshi.etf.tcc.EtfTccBeanUtil;
import cn.panshi.etf.tcc.EtfTccDao;

@Configuration
public class EtfTccRedisExpireEventListener {
	static Logger logger = Logger.getLogger(EtfTccRedisExpireEventListener.class);
	@Resource
	EtfTccBeanUtil etfTccBeanUtil;
	@Resource
	EtfRobBeanUtil etfRobBeanUtil;
	@Resource
	EtfTccDao etfTccDao;

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
				if (matchingKey) {
					logger.debug(String.format("redis queue: %s, body: %s", new String(channel), expireKey));
					etfRobBeanUtil.processEtfTimerExpire(expireKey);
					return;
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}

	}
}