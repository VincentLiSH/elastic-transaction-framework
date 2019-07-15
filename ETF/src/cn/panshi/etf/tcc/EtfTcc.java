package cn.panshi.etf.tcc;

public @interface EtfTcc {
	@SuppressWarnings("rawtypes")
	Class<? extends Enum> transEnumClazz();

	String transEnumValue();
}
