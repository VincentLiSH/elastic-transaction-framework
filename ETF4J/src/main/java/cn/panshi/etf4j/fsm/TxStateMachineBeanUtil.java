package cn.panshi.etf4j.fsm;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

@SuppressWarnings("rawtypes")
@Component
public class TxStateMachineBeanUtil implements BeanPostProcessor {
	static Logger logger = Logger.getLogger(TxStateMachineBeanUtil.class);

	Map<Class<? extends Enum>, Object> fsmBeanMap = new HashMap<>();
	Map<String, Method> fsmMethodMap = new HashMap<>();

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Ann4TxStateMachine ann4TxStateMachine = bean.getClass().getAnnotation(Ann4TxStateMachine.class);
		if (ann4TxStateMachine == null) {
			return bean;
		} else {
			logger.debug("扫描到Ann4TxStateMachine组件：" + ann4TxStateMachine.flowMemo());
		}

		Map<String, Object> etfBeanMethodMap = new HashMap<>();
		Method[] methods = ReflectionUtils.getAllDeclaredMethods(bean.getClass());
		if (methods != null) {
			for (Method method : methods) {
				Ann4TxStateMachineAction ann = AnnotationUtils.findAnnotation(method, Ann4TxStateMachineAction.class);
				if (ann == null) {
					continue;
				}
				boolean firstTimeCheckTheMethod = true;
				if (etfBeanMethodMap.get(method.getName()) != null) {
					firstTimeCheckTheMethod = false;
				} else {
					etfBeanMethodMap.put(method.getName(), "");
				}

				try {
					Enum.valueOf(ann4TxStateMachine.stateEnumClazz(), ann.from());
					Enum.valueOf(ann4TxStateMachine.stateEnumClazz(), ann.to());
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					throw new RuntimeException(e);
				}

				String key = ann4TxStateMachine.stateEnumClazz().getName() + ":" + ann.from() + "~" + ann.to();
				if (fsmMethodMap.get(key) != null && firstTimeCheckTheMethod) {
					throw new RuntimeException("TxStateMachine [" + key + "]被重复定义了");
				}
				fsmBeanMap.put(ann4TxStateMachine.stateEnumClazz(), bean);//TODO 有可能在多个StateMachine定义中使用同一个StateEnum 导致覆盖
				fsmMethodMap.put(key, method);
			}
		}
		return bean;
	}

	public AbsTxStateMachineActionCallback getActionCallback(Class<? extends Enum> stateEnumClazz, String from,
			String to) {
		String key = stateEnumClazz.getName() + ":" + from + "~" + to;
		Object target = fsmBeanMap.get(stateEnumClazz);
		Method method = fsmMethodMap.get(key);
		if (method == null) {
			String error = "StateMachine["+ key + "]转换不存在!";
			logger.error(error);
			throw new RuntimeException(error);
		}

		try {
			Object object = ReflectionUtils.invokeMethod(method, target);
			return (AbsTxStateMachineActionCallback) object;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	public Object getStateMachineClazz(Class<? extends Enum> stateEnumClazz) {
		return fsmBeanMap.get(stateEnumClazz);
	}

}