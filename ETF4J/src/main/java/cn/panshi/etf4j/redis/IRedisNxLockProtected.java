package cn.panshi.etf4j.redis;

public interface IRedisNxLockProtected {
	void doBizOnLockSuccess();

	void onLockFailure();

	void onFinalReleaseLock(Long unlock);
}