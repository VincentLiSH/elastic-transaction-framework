package cn.panshi.etf.robust;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EtfRobustTx {
	@SuppressWarnings("rawtypes")
	Class<? extends Enum> transEnumClazz();

	String transEnumValue();

	/**
	 * 默认0不重试，大于0 则失败后会自动重试
	 */
	int retryMaxTimes() default 0;// 默认0 不做失败重试

	/**
	 * 不能设置的过小（小于20）
	 */
	int retryFirstDelaySeconds() default 60;

	int retryIntervalSeconds() default 300;

	/**
	 * 默认0不做交易查询，大于0 则交易完成后自动查询交易结果，需要具体交易类型实现查询和结果处理逻辑
	 */
	int queryMaxTimes() default 0;// 默认0 不做交易查询

	/**
	 * 不能设置的过小（小于20）
	 */
	int queryFirstDelaySeconds() default 60;

	int queryIntervalSeconds() default 300;

}
