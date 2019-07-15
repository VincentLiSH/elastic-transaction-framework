package cn.panshi.etf.tcc;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import com.alibaba.fastjson.JSONObject;

import cn.panshi.etf.tcc.EtfTccDaoRedis.ETF_TCC_KEYS;

@Component
public class EtfTccBeanUtil implements BeanPostProcessor {
	static Logger log = LoggerFactory.getLogger(EtfTccBeanUtil.class);
	@Resource
	ThreadPoolTaskExecutor executor;
	@Resource
	EtfTccDao etfTccDao;

	Map<String, Object> etfTransBeanMap = new HashMap<>();
	Map<String, Method> etpTransBeanMethodMap = new HashMap<>();

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Map<String, Object> etfBeanMethodMap = new HashMap<>();
		Method[] methods = ReflectionUtils.getAllDeclaredMethods(bean.getClass());
		if (methods != null) {
			for (Method method : methods) {
				EtfTcc ann = AnnotationUtils.findAnnotation(method, EtfTcc.class);
				if (ann == null) {
					continue;
				}
				boolean firstTimeCheckTheMethod = true;
				if (etfBeanMethodMap.get(method.getName()) != null) {
					firstTimeCheckTheMethod = false;
				} else {
					etfBeanMethodMap.put(method.getName(), "");
				}

				String key = ann.transEnumClazz().getName() + "." + ann.transEnumValue();
				if (etfTransBeanMap.get(key) != null && firstTimeCheckTheMethod) {
					throw new RuntimeException("TCC api[" + key + "]被重复定义了:[" + beanName + "." + method.getName()
							+ "]和[" + etfTransBeanMap.get(key).getClass().getName() + "."
							+ etpTransBeanMethodMap.get(key).getName() + "]");
				}
				etfTransBeanMap.put(key, bean);
				etpTransBeanMethodMap.put(key, method);
			}
		}
		return bean;
	}

	private static final LocalVariableTableParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

	public void invokeEtfBean(String tccEnumClazz, String tccTransType, JSONObject paramJsonObj) {
		String key = tccEnumClazz + "." + tccTransType;
		Object target = etfTransBeanMap.get(key);
		Method method = etpTransBeanMethodMap.get(key);

		Parameter[] methodParamArray = method.getParameters();

		Object[] argArry2InvokeTarget = new Object[methodParamArray.length];

		String[] paraNames = parameterNameDiscoverer.getParameterNames(method);

		for (int i = 0; i < methodParamArray.length; i++) {
			Parameter methodParam = methodParamArray[i];
			argArry2InvokeTarget[i] = JSONObject.toJavaObject(paramJsonObj.getJSONObject(paraNames[i]),
					methodParam.getType());
		}
		Object object = ReflectionUtils.invokeMethod(method, target, argArry2InvokeTarget);
		log.debug(object + "");
	}

	public void processTccTimerExpire(String expireKey) {
		if (StringUtils.startsWith(expireKey, ETF_TCC_KEYS.ETF_TCC_TIMER_CANCEL.name())) {
			String bizId = expireKey.substring(expireKey.indexOf("#") + 1);

			String transTypeEnumClazz = expireKey.substring(expireKey.lastIndexOf(":") + 1, expireKey.indexOf("#"));

			List<EtfTccRecordStep> trStepList = etfTccDao.queryTccRecordStepList(transTypeEnumClazz, bizId);
			log.debug("查找到需要cancel的TCC[" + transTypeEnumClazz + "#" + bizId + "] step" + trStepList.size() + "个");
			for (EtfTccRecordStep step : trStepList) {
				executor.submit(new Runnable() {
					@Override
					public void run() {
						EtfTccRecordStep tr = etfTccDao.loadTccTransRecordStep(transTypeEnumClazz,
								step.getTccEnumValue(), bizId);
						JSONObject paramJsonObj = JSONObject.parseObject(tr.getBizStateJson());

						EtfTccAop.setCurrTccTryStage();
						EtfTccAop.setCURR_INVOKE_BIZ_ID(bizId);
						EtfTccAop.setCURR_INVOKE_TCC_ENUM_CLAZZ_NAME(transTypeEnumClazz);
						EtfTccAop.setCURR_INVOKE_TCC_ENUM_VALUE(step.getTccEnumValue());

						EtfTccAop.setCurrTccCancelStage();
						invokeEtfBean(transTypeEnumClazz, step.getTccEnumValue(), paramJsonObj);
					}
				});
			}
		} else if (StringUtils.startsWith(expireKey, ETF_TCC_KEYS.ETF_TCC_TIMER_CONFIRM.name())) {
			String bizId = expireKey.substring(expireKey.indexOf("#") + 1);

			String transTypeEnumClazz = expireKey.substring(expireKey.lastIndexOf(":") + 1, expireKey.indexOf("#"));

			List<EtfTccRecordStep> trStepList = etfTccDao.queryTccRecordStepList(transTypeEnumClazz, bizId);
			for (EtfTccRecordStep step : trStepList) {
				executor.submit(new Runnable() {
					@Override
					public void run() {
						EtfTccRecordStep tr = etfTccDao.loadTccTransRecordStep(transTypeEnumClazz,
								step.getTccEnumValue(), bizId);
						JSONObject paramJsonObj = JSONObject.parseObject(tr.getBizStateJson());

						EtfTccAop.setCurrTccTryStage();
						EtfTccAop.setCURR_INVOKE_BIZ_ID(bizId);
						EtfTccAop.setCURR_INVOKE_TCC_ENUM_CLAZZ_NAME(transTypeEnumClazz);
						EtfTccAop.setCURR_INVOKE_TCC_ENUM_VALUE(step.getTccEnumValue());

						EtfTccAop.setCurrTccConfirmStage();
						invokeEtfBean(transTypeEnumClazz, step.getTccEnumValue(), paramJsonObj);
					}
				});
			}

		}
	}
}
