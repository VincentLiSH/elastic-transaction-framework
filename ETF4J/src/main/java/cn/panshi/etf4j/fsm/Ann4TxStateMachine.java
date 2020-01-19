package cn.panshi.etf4j.fsm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Ann4TxStateMachine {
	String flowMemo();

	@SuppressWarnings("rawtypes")
	Class<? extends Enum> stateEnumClazz();

}