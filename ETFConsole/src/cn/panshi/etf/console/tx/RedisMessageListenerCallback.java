package cn.panshi.etf.console.tx;

public interface RedisMessageListenerCallback {
	void callBack(String msgChannel, String msgBody);
}
