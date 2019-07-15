package cn.panshi.etf.tcc;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

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