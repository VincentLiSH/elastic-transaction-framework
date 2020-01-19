package cn.panshi.etf4j.fsm;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import cn.panshi.etf4j.fsm.TestStateMachine.TestEnum;

@Component
@Ann4TxStateMachine(flowMemo="测试流程",stateEnumClazz=TestEnum.class)
public class TestStateMachine {
	Logger logger=Logger.getLogger(TestStateMachine.class);
	
	enum TestEnum{
		s1,s2,s3,s4;
	}
	
	@Ann4TxStateMachineAction(actionMemo="a12",from="s1",to="s2")
	public AbsTxStateMachineActionCallback a12() {
		return new AbsTxStateMachineActionCallback (){

			@Override
			boolean actionReady(String key) {
				return true;
			}

			@Override
			void doAction(String key) {
				logger.debug(key+" doAction a12");
			}
			
		};
	}
	
	@Ann4TxStateMachineAction(actionMemo="a23",from="s2",to="s3")
	public AbsTxStateMachineActionCallback a23() {
		return new AbsTxStateMachineActionCallback (){
			
			@Override
			boolean actionReady(String key) {
				return true;
			}
			
			@Override
			void doAction(String key) {
				logger.debug(key+" doAction a23");
			}
			
		};
	}
	
	@Ann4TxStateMachineAction(actionMemo="a34",from="s3",to="s4")
	public AbsTxStateMachineActionCallback a34() {
		return new AbsTxStateMachineActionCallback (){
			
			@Override
			boolean actionReady(String key) {
				return true;
			}
			
			@Override
			void doAction(String key) {
				logger.debug(key+" doAction a34");
			}
			
		};
	}

}