package cn.panshi.etf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

public class RedisUtil {
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Long rightPopAndLeftPush(RedisTemplate redisTemplate, String popList, String pushList,
			String pushValue) {
		String UNLOCK_LUA = "redis.call(\"lpush\",KEYS[2],ARGV[1])                                      "
				+ "          return redis.call(\"rpop\",KEYS[1])                                                                         ";
		Long result = (Long) redisTemplate.execute(new RedisCallback<Long>() {
			@Override
			public Long doInRedis(RedisConnection connection) throws DataAccessException {
				Object nativeConnection = connection.getNativeConnection();
				if (nativeConnection instanceof JedisCluster) {
					return (Long) ((JedisCluster) nativeConnection).eval(UNLOCK_LUA, 2, popList, pushList, pushValue);
				}

				else if (nativeConnection instanceof Jedis) {
					return (Long) ((Jedis) nativeConnection).eval(UNLOCK_LUA, 2, popList, pushList, pushValue);
				}
				return 0L;
			}
		});
		return result;
	}
}