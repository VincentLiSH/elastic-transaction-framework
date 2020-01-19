package cn.panshi.etf4j.fsm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Ann4TxStateMachineAction {
	String actionMemo();

	String from();

	String to();
}
