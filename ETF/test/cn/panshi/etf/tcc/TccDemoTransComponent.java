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
					System.out.println("step1 try..." + vo.getCode());
					throw new RuntimeException("step1 try 失败");
				}

				@Override
				protected void tccConfirm() {
					System.out.println("confirm1..." + vo.getCode());
				}

				@Override
				protected void tccCancel() {
					System.out.println("cancel1..." + vo.getCode());
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
					System.out.println("try2..." + vo.getCode());
				}

				@Override
				protected void tccConfirm() {
					System.out.println("confirm2..." + vo.getCode());
				}

				@Override
				protected void tccCancel() {
					System.out.println("cancel2..." + vo.getCode());
				}
			}.executeEtfTcc();
		} catch (EtfException4LockConcurrent e) {
			e.printStackTrace();
		}
	}

}
