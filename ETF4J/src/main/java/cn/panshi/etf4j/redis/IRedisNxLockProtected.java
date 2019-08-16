package cn.panshi.etf4j.redis;

/**
 * 用于封装需要redis nx排它锁保护的业务逻辑，用于AbstractRedisLockTemplate.doBizWithinLockProtection回调。
 * 适用于简单业务场景（无返回值 或单一返回值逻辑）
 */
public interface IRedisNxLockProtected {
	void doBizOnLockSuccess();

	void onLockFailure();

	void onFinalReleaseLock(Long unlock);
}