package cn.panshi.etf.tcc;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import cn.panshi.etf.core.EtfException4LockConcurrent;

@Component
public class TccDemoTransComponent {
	static Logger log = LoggerFactory.getLogger(TccDemoTransComponent.class);
	@Resource
	EtfTccDaoRedis etfTccDaoRedis;
	@EtfTcc(transEnumClazz = TccDemoEnum.class, transEnumValue = "step1")
	public void tccStep1(TccDemoVo vo) {
		try {
			new EtfTccTransTemplate<TccDemoEnum>(etfTccDaoRedis) {

				@Override
				protected String calcTccTransBizId() {
					return vo.getCode();
				}

				@Override
				protected void tccTry() {
					System.out.println("try1...");
				}

				@Override
				protected void tccConfirm() {
					System.out.println("confirm1...");
				}

				@Override
				protected void tccCancel() {
					System.out.println("cancel1...");
				}
			}.executeEtfTcc();
		} catch (EtfException4LockConcurrent e) {
			e.printStackTrace();
		}
	}

	@EtfTcc(transEnumClazz = TccDemoEnum.class, transEnumValue = "step2")
	public void tccStep2(TccDemoVo vo) {
		try {
			new EtfTccTransTemplate<TccDemoEnum>(etfTccDaoRedis) {

				@Override
				protected String calcTccTransBizId() {
					return vo.getCode();
				}

				@Override
				protected void tccTry() {
					System.out.println("try2...");
				}

				@Override
				protected void tccConfirm() {
					System.out.println("confirm2...");
				}

				@Override
				protected void tccCancel() {
					System.out.println("cancel2...");
				}
			}.executeEtfTcc();
		} catch (EtfException4LockConcurrent e) {
			e.printStackTrace();
		}
	}

}
