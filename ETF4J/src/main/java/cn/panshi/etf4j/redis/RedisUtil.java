package cn.panshi.etf4j.redis;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

public class RedisUtil {
	/**
	 * redis自带的rpoplpush只能在两个list之间移动元素；
	 * rightPopAndLeftPushAnotherValue实现pop一个list同时向另一个list中push一个特定的value，只是把前一个list当做计数器，返回pop的值
	 * poplist被pop空以后就会返回null
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object rightPopAndLeftPushAnotherValue(RedisTemplate redisTemplate, String popList, String pushList,
			String pushValue) {
		String LUA = "redis.call(\"lpush\",KEYS[2],ARGV[1]) "//先lpush pushList pushValue
				+ "   return redis.call(\"rpop\",KEYS[1])   ";
		Object result = redisTemplate.execute(new RedisCallback<Object>() {
			@Override
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				Object nativeConnection = connection.getNativeConnection();
				if (nativeConnection instanceof JedisCluster) {
					return ((JedisCluster) nativeConnection).eval(LUA, 2, popList, pushList, "\"" + pushValue + "\"");
				}

				else if (nativeConnection instanceof Jedis) {
					return ((Jedis) nativeConnection).eval(LUA, 2, popList, pushList, "\"" + pushValue + "\"");
				}
				throw new RuntimeException("unsupported nativeConnection " + nativeConnection.getClass());
			}
		});
		return result;
	}
}