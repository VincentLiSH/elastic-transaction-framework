package cn.panshi.etf.core;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

/**
 * 拦截ETF交易组件API调用（@EtfAnnTransApi），保存入参、和API annotation到ThreadLocal 供EtfTemplate使用
 */
@Component
@Aspect
public class EtfAop {
	static Logger log = LoggerFactory.getLogger(EtfAop.class);

	private final static ThreadLocal<EtfAnnTransApi> CURR_INVOK_API_ANN = new ThreadLocal<>();

	private final static ThreadLocal<String> CURR_INVOKE_BIZ_ID = new ThreadLocal<>();
	/**
	 * 用于ETF保存交易调用现场，以便在retry和交易查询时恢复
	 */
	private final static ThreadLocal<JSONObject> CURR_INVOKE_INPUT_PARAM = new ThreadLocal<>();

	private final static ThreadLocal<String> CURR_INVOKE_RETRY_TIMER_KEY = new ThreadLocal<>();

	private final static ThreadLocal<String> CURR_INVOKE_QUERY_TIMER_KEY = new ThreadLocal<>();

	@Around("@annotation(cn.panshi.etf.core.EtfAnnTransApi)")
	public Object around(ProceedingJoinPoint joinPoint) throws Throwable {

		CURR_INVOKE_INPUT_PARAM.set(calcInputParamJsonIbj(joinPoint));

		CURR_INVOK_API_ANN.set(calcMethodAnn(joinPoint));

		Object result;
		try {
			result = joinPoint.proceed();
		} finally {
			CURR_INVOK_API_ANN.remove();

			CURR_INVOKE_INPUT_PARAM.remove();

			CURR_INVOKE_BIZ_ID.remove();

			CURR_INVOKE_RETRY_TIMER_KEY.remove();

			CURR_INVOKE_QUERY_TIMER_KEY.remove();
		}

		return result;
	}

	private EtfAnnTransApi calcMethodAnn(ProceedingJoinPoint joinPoint) {
		MethodSignature sig = ((MethodSignature) joinPoint.getSignature());
		EtfAnnTransApi etfAnn = sig.getMethod().getAnnotation(EtfAnnTransApi.class);
		return etfAnn;
	}

	private JSONObject calcInputParamJsonIbj(ProceedingJoinPoint joinPoint) {
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

	public static EtfAnnTransApi getCurrEtfApiAnn() {
		return CURR_INVOK_API_ANN.get();
	}

	public static JSONObject getCurrEtfInvokeParam() {
		return CURR_INVOKE_INPUT_PARAM.get();
	}

	public static void setCurrEtfBizId(String id) {
		CURR_INVOKE_BIZ_ID.set(id);
	}

	public static String getCurrEtfBizId() {
		return CURR_INVOKE_BIZ_ID.get();
	}

	public static void setCurrEtfTransQueryTimerKey(String queryCount) {
		CURR_INVOKE_QUERY_TIMER_KEY.set(queryCount);
	}

	public static String getCurrEtfTransQueryTimerKey() {
		return CURR_INVOKE_QUERY_TIMER_KEY.get();
	}

	public static void setCurrEtfTransRetryTimerKey(String queryCount) {
		CURR_INVOKE_RETRY_TIMER_KEY.set(queryCount);
	}

	public static String getCurrEtfTransRetryTimerKey() {
		return CURR_INVOKE_RETRY_TIMER_KEY.get();
	}
}