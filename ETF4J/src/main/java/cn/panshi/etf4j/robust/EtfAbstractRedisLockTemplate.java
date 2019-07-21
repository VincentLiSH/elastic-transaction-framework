package cn.panshi.etf4j.robust;

import org.apache.commons.lang.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisCommands;

@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class EtfAbstractRedisLockTemplate {
	RedisTemplate redisTemplate;
	int expireSeconds;
	
	String lockToken;

	public EtfAbstractRedisLockTemplate(RedisTemplate redisTemplate, int expireSeconds, String lockToken) {
		super();
		this.redisTemplate = redisTemplate;
		this.expireSeconds = expireSeconds;
		this.lockToken = lockToken; 
	}

	public final boolean lock() {
		final String key = constructKey();

		final String result = (String) redisTemplate.execute(new RedisCallback<String>() {
			@Override
			public String doInRedis(RedisConnection connection) throws DataAccessException {
				JedisCommands commands = (JedisCommands) connection.getNativeConnection();
				return commands.set(key, lockToken, "NX", "EX", expireSeconds);
			}
		});

		boolean lockSuccess = (StringUtils.equals(result, "OK"));

		return lockSuccess;
	}

	protected abstract String constructKey();

	public final Long unlock() {
		String UNLOCK_LUA = "if redis.call(\"get\",KEYS[1]) == ARGV[1]                                      "
				+ "              then  return redis.call(\"del\",KEYS[1])\r\n                               "
				+ "                else  return 0                                                           "
				+ "          end                                                                            ";
		Long result = (Long) redisTemplate.execute(new RedisCallback<Long>() {
			@Override
			public Long doInRedis(RedisConnection connection) throws DataAccessException {
				Object nativeConnection = connection.getNativeConnection();
				if (nativeConnection instanceof JedisCluster) {
					return (Long) ((JedisCluster) nativeConnection).eval(UNLOCK_LUA, 1, constructKey(), lockToken);
				}

				else if (nativeConnection instanceof Jedis) {
					return (Long) ((Jedis) nativeConnection).eval(UNLOCK_LUA, 1, constructKey(), lockToken);
				}
				return 0L;
			}
		});

		return result;
	}
}