package cn.panshi.etf4j.tcc;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import cn.panshi.etf4j.tcc.EtfTccDaoRedis;
import cn.panshi.etf4j.tcc.EtfTccException4PrepareStage;
import cn.panshi.etf4j.tcc.EtfTccException4StartStage;
import cn.panshi.etf4j.tcc.TccTransPrepareStatement;
import cn.panshi.etf4j.tcc.TccTransStarter;

@Component
public class TccDemoStarterComponent {
	@Resource
	TccDemoTransComponent tccDemoTransComponent;

	@Resource
	EtfTccDaoRedis etfTccDaoRedis;

	public void startTccFlow1() throws EtfTccException4PrepareStage, EtfTccException4StartStage {
		TccDemoVo vo = new TccDemoVo();
		vo.setCode("unit test");

		TccTransStarter<TccDemoEnum> starter = new TccTransStarter<TccDemoEnum>(etfTccDaoRedis);

		starter.prepareTccTrans(new TccTransPrepareStatement() {
			@Override
			public void doPrepare() {
				tccDemoTransComponent.tccStep1(vo);
			}
		});

		starter.prepareTccTrans(new TccTransPrepareStatement() {
			@Override
			public void doPrepare() {
				tccDemoTransComponent.tccStep2(vo);
			}
		});

		starter.startTccTransList();
	}

}