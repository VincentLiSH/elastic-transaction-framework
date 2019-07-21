package cn.panshi.etf4j.tcc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EtfTcc {
	@SuppressWarnings("rawtypes")
	Class<? extends Enum> transEnumClazz();

	String transEnumValue();
}
