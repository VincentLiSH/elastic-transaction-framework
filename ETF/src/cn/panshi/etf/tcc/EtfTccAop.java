package cn.panshi.etf.tcc;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import cn.panshi.etf.tcc.EtfTccTransTemplate.TCC_TRANS_STAGE;

@Component
@Aspect
public class EtfTccAop {
	static Logger log = Logger.getLogger(EtfTccAop.class);
	private final static ThreadLocal<TCC_TRANS_STAGE> TCC_TRANS_PREPARE_STAGE = new ThreadLocal<>();

	public static void setCurrTccPrepareStage() {
		TCC_TRANS_PREPARE_STAGE.set(TCC_TRANS_STAGE.tcc_prepare);
	}

	public static void setCurrTccTryStage() {
		TCC_TRANS_PREPARE_STAGE.set(TCC_TRANS_STAGE.tcc_try);
	}

	public static void setCurrTccCancelStage() {
		TCC_TRANS_PREPARE_STAGE.set(TCC_TRANS_STAGE.tcc_cancel);
	}

	public static void setCurrTccConfirmStage() {
		TCC_TRANS_PREPARE_STAGE.set(TCC_TRANS_STAGE.tcc_confirm);
	}

	public static TCC_TRANS_STAGE getCurrTccStage() {
		return TCC_TRANS_PREPARE_STAGE.get();
	}

	private final static ThreadLocal<String> CURR_INVOKE_BIZ_ID = new ThreadLocal<>();

	public static String getCurrBizId() {
		return CURR_INVOKE_BIZ_ID.get();
	}

	private final static ThreadLocal<EtfTcc> CURR_INVOK_API_ANN = new ThreadLocal<>();

	private final static ThreadLocal<JSONObject> CURR_INVOKE_INPUT_PARAM = new ThreadLocal<>();

	private final static ThreadLocal<String> CURR_INVOKE_TCC_ENUM_CLAZZ_NAME = new ThreadLocal<>();
	private final static ThreadLocal<String> CURR_INVOKE_TCC_ENUM_VALUE = new ThreadLocal<>();

	public static String getCurrTccTransEnumClazzName() {
		return CURR_INVOKE_TCC_ENUM_CLAZZ_NAME.get();
	}

	@Around("@annotation(cn.panshi.etf.tcc.EtfTcc)")
	public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
		EtfTcc ann = calcMethodAnn(joinPoint);
		if (TCC_TRANS_PREPARE_STAGE.get() != null) {

			TccTransStarter.setTCC_ENUM_TYPE_OF_JUST_PREPARED_TRANS(ann.transEnumClazz());
			TccTransStarter.setTCC_ENUM_VALUE_OF_JUST_PREPARED_TRANS(ann.transEnumValue());
		}

		CURR_INVOKE_INPUT_PARAM.set(calcInputParamJsonObj(joinPoint));

		CURR_INVOK_API_ANN.set(ann);

		try {
			return joinPoint.proceed();
		} finally {

			if (TCC_TRANS_PREPARE_STAGE.get() == null) {

				CURR_INVOK_API_ANN.remove();

				CURR_INVOKE_INPUT_PARAM.remove();

				CURR_INVOKE_BIZ_ID.remove();
			} else {
				log.debug("TCC_TRANS_PREPARE_STAGE准备阶段 不删除TCC上下文");
			}

		}
	}

	private EtfTcc calcMethodAnn(ProceedingJoinPoint joinPoint) {
		MethodSignature sig = ((MethodSignature) joinPoint.getSignature());
		EtfTcc etfAnn = sig.getMethod().getAnnotation(EtfTcc.class);
		return etfAnn;
	}

	private JSONObject calcInputParamJsonObj(ProceedingJoinPoint joinPoint) {
		MethodSignature sig = ((MethodSignature) joinPoint.getSignature());
		String[] argNames = sig.getParameterNames();
		JSONObject json = new JSONObject();
		for (int i = 0; i < joinPoint.getArgs().length; i++) {
			String argName = argNames[i];
			Object argValue = joinPoint.getArgs()[i];
			json.put(argName, argValue);
		}
		return json;
	}

	public static JSONObject getCURR_INVOKE_INPUT_PARAM() {
		return CURR_INVOKE_INPUT_PARAM.get();
	}

	public static void setCURR_INVOKE_BIZ_ID(String tccTransBizId) {
		CURR_INVOKE_BIZ_ID.set(tccTransBizId);
	}

	public static void setCURR_INVOKE_TCC_ENUM_CLAZZ_NAME(String transTypeEnumClazz) {
		CURR_INVOKE_TCC_ENUM_CLAZZ_NAME.set(transTypeEnumClazz);
	}

	public static void setCURR_INVOKE_TCC_ENUM_VALUE(String tccTransEnumValue) {
		CURR_INVOKE_TCC_ENUM_VALUE.set(tccTransEnumValue);
	}

	public static String getCURR_INVOKE_TCC_ENUM_VALUE() {
		return CURR_INVOKE_TCC_ENUM_VALUE.get();
	}
}