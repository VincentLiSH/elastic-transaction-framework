package cn.panshi.etf.console.tx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

@Component
public class RedisMessageListener implements MessageListener {
	private final Logger logger = Logger.getLogger(getClass());
	Map<String, RedisMessageListenerCallback> callbackMap = new HashMap<>();
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	/**
	 * @return the redisTemplate
	 */
	public RedisTemplate<String, Object> getRedisTemplate() {
		return redisTemplate;
	}

	/**
	 * @param redisTemplate the redisTemplate to set
	 */
	public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	private static final StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

	@Override
	public void onMessage(Message message, byte[] pattern) {
		byte[] body = message.getBody();
		String msgBody = stringRedisSerializer.deserialize(body);
		System.out.println(msgBody);
		byte[] channel = message.getChannel();
		String msgChannel = stringRedisSerializer.deserialize(channel);
		System.out.println(msgChannel);
		String msgPattern = new String(pattern);
		System.out.println(msgPattern);
		Object[] keyArray = callbackMap.keySet().toArray();

		logger.debug("callbackMapcallbackMapcallbackMapcallbackMap:" + callbackMap);
		for (Object key : keyArray) {
			RedisMessageListenerCallback callback = callbackMap.remove(key);
			try {
				callback.callBack(msgChannel, msgBody);
			} catch (Exception e) {
			}
		}
	}

	public void addCallback(RedisMessageListenerCallback callback) {
		callbackMap.put(UUID.randomUUID().toString(), callback);
	}

}
