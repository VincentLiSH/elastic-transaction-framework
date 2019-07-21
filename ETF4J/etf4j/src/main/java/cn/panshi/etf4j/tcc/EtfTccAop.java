package cn.panshi.etf4j.tcc;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import cn.panshi.etf4j.tcc.EtfTccTransTemplate.TCC_TRANS_STAGE;

@Component
@Aspect
public class EtfTccAop {
	static Logger log = Logger.getLogger(EtfTccAop.class);
	private final static ThreadLocal<TCC_TRANS_STAGE> TL_TCC_CURR_STAGE = new ThreadLocal<>();
	private final static ThreadLocal<String> TL_TCC_CURR_BIZ_ID = new ThreadLocal<>();

	private final static ThreadLocal<JSONObject> TL_TCC_CURR_INPUT_PARAM = new ThreadLocal<>();
	private final static ThreadLocal<String> TL_TCC_CURR_TRANS_ENUM_CLAZZ_NAME = new ThreadLocal<>();
	private final static ThreadLocal<String> TL_TCC_CURR_TRANS_ENUM_VALUE = new ThreadLocal<>();

	@Around("@annotation(cn.panshi.etf4j.tcc.EtfTcc)")
	public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
		EtfTcc ann = calcMethodAnn(joinPoint);

		JSONObject inputParamJsonObj = calcInputParamJsonObj(joinPoint);

		if (TL_TCC_CURR_STAGE.get() != null) {
			TccTransStarter.setTCC_ENUM_TYPE_OF_JUST_PREPARED_TRANS(ann.transEnumClazz());
			TccTransStarter.setTCC_ENUM_VALUE_OF_JUST_PREPARED_TRANS(ann.transEnumValue());
			TccTransStarter.setTCC_CURR_INPUT_PARAM(inputParamJsonObj);
		}

		TL_TCC_CURR_INPUT_PARAM.set(inputParamJsonObj);
		TL_TCC_CURR_TRANS_ENUM_CLAZZ_NAME.set(ann.transEnumClazz().getName());
		TL_TCC_CURR_TRANS_ENUM_VALUE.set(ann.transEnumValue());

		try {
			return joinPoint.proceed();
		} finally {
			TL_TCC_CURR_STAGE.remove();
			TL_TCC_CURR_BIZ_ID.remove();

			TL_TCC_CURR_INPUT_PARAM.remove();
			TL_TCC_CURR_TRANS_ENUM_CLAZZ_NAME.remove();
			TL_TCC_CURR_TRANS_ENUM_VALUE.remove();
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

	public static void setTccCurrStagePrepare() {
		TL_TCC_CURR_STAGE.set(TCC_TRANS_STAGE.tcc_prepare);
	}

	public static void setTccCurrStageTry() {
		TL_TCC_CURR_STAGE.set(TCC_TRANS_STAGE.tcc_try);
	}

	public static void setTccCurrStageCancel() {
		TL_TCC_CURR_STAGE.set(TCC_TRANS_STAGE.tcc_cancel);
	}

	public static void setTccCurrStageConfirm() {
		TL_TCC_CURR_STAGE.set(TCC_TRANS_STAGE.tcc_confirm);
	}

	public static TCC_TRANS_STAGE getCurrTccStage() {
		return TL_TCC_CURR_STAGE.get();
	}

	public static String getTCC_CURR_BIZ_ID() {
		return TL_TCC_CURR_BIZ_ID.get();
	}

	public static void setTCC_CURR_BIZ_ID(String tccTransBizId) {
		TL_TCC_CURR_BIZ_ID.set(tccTransBizId);
	}

	public static String getTCC_CURR_TRANS_ENUM_CLAZZ_NAME() {
		return TL_TCC_CURR_TRANS_ENUM_CLAZZ_NAME.get();
	}

	public static JSONObject getTCC_CURR_INPUT_PARAM() {
		return TL_TCC_CURR_INPUT_PARAM.get();
	}

	public static String getTCC_CURR_ENUM_VALUE() {
		return TL_TCC_CURR_TRANS_ENUM_VALUE.get();
	}
}