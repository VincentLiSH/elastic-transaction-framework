package cn.panshi.etf4j.fsm;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

@SuppressWarnings("rawtypes")
public class TxStateMachineDriver {
	Logger logger = Logger.getLogger(TxStateMachineDriver.class);
	
	private TxStateMachineBeanUtil txStateMachineBeanUtil;
	
	private TxStateMachineRepo txStateMachineRepo;
	
	public TxStateMachineDriver(TxStateMachineBeanUtil txStateMachineBeanUtil, TxStateMachineRepo txStateMachineRepo) {
		super();
		this.txStateMachineBeanUtil = txStateMachineBeanUtil;
		this.txStateMachineRepo = txStateMachineRepo;
	}

	public boolean load(Class<? extends Enum> stateEnumClazz, String key) {
		return false;
	}

	public List<String> getTransferList(Class<? extends Enum> stateEnumClazz, String key) {
		List<String> result=new ArrayList<String>();
		String currentState = getCurrentState(stateEnumClazz, key);
		Object stateMachine=txStateMachineBeanUtil.getStateMachineClazz(stateEnumClazz);
		
		Method[] methods = ReflectionUtils.getAllDeclaredMethods(stateMachine.getClass());
		if (methods != null) {
			for (Method method : methods) {
				Ann4TxStateMachineAction ann = AnnotationUtils.findAnnotation(method, Ann4TxStateMachineAction.class);
				if (ann == null) {
					continue;
				}
				if(StringUtils.equals(ann.from(),currentState)) {
					result.add(ann.to());
				}
			}
		}
		return result;
	}

	public void transfer(Class<? extends Enum> stateEnumClazz, String key, String actionCode) {
		String currentState = getCurrentState(stateEnumClazz, key);
		AbsTxStateMachineActionCallback actionCallback = txStateMachineBeanUtil.getActionCallback(stateEnumClazz,
				currentState, actionCode);
		
		if(actionCallback==null) {
			String error = "StateMachine[" + stateEnumClazz.getName() + ":" + key + "]不存在转换" + currentState + "~"
					+ actionCode + "!";
			logger.error(error);
			throw new RuntimeException(error);
		}
		
		if (actionCallback.actionReady(key)) {
			actionCallback.doAction(key);
		} else {
			logger.warn("StateMachine[" + stateEnumClazz.getName() + ":" + key + "]的当前转换" + currentState + "~"
					+ actionCode + "还不具备执行条件!");
		}
		
		txStateMachineRepo.updateState(stateEnumClazz, key,actionCode);
	}

	public String getCurrentState(Class<? extends Enum> stateEnumClazz, String key) {
		return txStateMachineRepo.getCurrentState(stateEnumClazz,key);
	}

}