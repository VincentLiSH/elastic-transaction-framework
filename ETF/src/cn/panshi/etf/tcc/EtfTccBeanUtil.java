package cn.panshi.etf.tcc;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import com.alibaba.fastjson.JSONObject;

@Component
public class EtfTccBeanUtil implements BeanPostProcessor {
	static Logger logger = Logger.getLogger(EtfTccBeanUtil.class);

	Map<String, Object> tccTransBeanMap = new HashMap<>();
	Map<String, Method> tccTransBeanMethodMap = new HashMap<>();

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
				if (tccTransBeanMap.get(key) != null && firstTimeCheckTheMethod) {
					throw new RuntimeException("TCC API[" + key + "]被重复定义了:[" + beanName + "." + method.getName()
							+ "]和[" + tccTransBeanMap.get(key).getClass().getName() + "."
							+ tccTransBeanMethodMap.get(key).getName() + "]");
				}
				tccTransBeanMap.put(key, bean);
				tccTransBeanMethodMap.put(key, method);
			}
		}
		return bean;
	}

	private static final LocalVariableTableParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

	public void invokeTccBean(String tccEnumClazz, String tccTransType, JSONObject paramJsonObj) {
		String key = tccEnumClazz + "." + tccTransType;
		Object target = tccTransBeanMap.get(key);
		Method method = tccTransBeanMethodMap.get(key);

		Parameter[] methodParamArray = method.getParameters();

		Object[] argArry2InvokeTarget = new Object[methodParamArray.length];

		String[] paraNames = parameterNameDiscoverer.getParameterNames(method);

		for (int i = 0; i < methodParamArray.length; i++) {
			Parameter methodParam = methodParamArray[i];
			argArry2InvokeTarget[i] = JSONObject.toJavaObject(paramJsonObj.getJSONObject(paraNames[i]),
					methodParam.getType());
		}
		Object object = ReflectionUtils.invokeMethod(method, target, argArry2InvokeTarget);
		System.out.println("无需输出此日志到logger，仅开发调试用。" + EtfTccBeanUtil.class.getName() + ".invokeMethod return" + object);
	}
}