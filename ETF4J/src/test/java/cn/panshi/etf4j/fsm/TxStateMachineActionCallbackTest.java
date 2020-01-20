package cn.panshi.etf4j.fsm;

import java.util.List;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cn.panshi.etf4j.fsm.TestStateMachine.TestEnum;

@SuppressWarnings({ "rawtypes" })
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring-fsm.xml" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TxStateMachineActionCallbackTest {
	TxStateMachineDriver tsmdriver;

	@Resource
	TxStateMachineBeanUtil txStateMachineBeanUtil;
	
	String currState="s1";

	@SuppressWarnings("unused")
	@Test
	public void test() {
		TxStateMachineRepo txStateMachineRepo = new TxStateMachineRepo() {

			@Override
			public void updateState(Class<? extends Enum> stateEnumClazz, String key, String actionCode) {
				currState=actionCode;
			}

			@Override
			public String getCurrentState(Class<? extends Enum> stateEnumClazz, String key) {
				return currState;
			}
		};

		tsmdriver = new TxStateMachineDriver(txStateMachineBeanUtil, txStateMachineRepo);

		String key = "UT4StateMachine";
		boolean machineRunning = tsmdriver.load(TestEnum.class, key);

		String currState = tsmdriver.getCurrentState(TestEnum.class, key);

		List<String> actionList = tsmdriver.getTransferList(TestEnum.class, key);

		tsmdriver.transfer(TestEnum.class, key, "s2");
		
		try {
			tsmdriver.transfer(TestEnum.class, key, "s3");
			Assert.fail("s2~s3 not available");
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			tsmdriver.transfer(TestEnum.class, key, "s4");
			Assert.fail("s2~s3 not available");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}