package cn.panshi.etf4j.tcc;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class TccDemoTransComponent {
	static Logger logger = Logger.getLogger(TccDemoTransComponent.class);
	@Resource
	EtfTccDaoRedis etfTccDaoRedis;

	@EtfTcc(transEnumClazz = TccDemoEnum.class, transEnumValue = "step1")
	public void tccStep1(TccDemoVo vo) {
		new EtfTccTransTemplate<TccDemoEnum>(etfTccDaoRedis) {

			@Override
			protected String calcTccTransBizId() {
				return vo.getCode();
			}

			@Override
			protected void tccTry() {
				logger.debug("step1 try..." + vo.getCode());
				throw new RuntimeException("step1 try 失败");
			}

			@Override
			protected void tccConfirm() {
				logger.debug("confirm1..." + vo.getCode());
				//throw new RuntimeException("step1 confirm 失败");
			}

			@Override
			protected void tccCancel() {
				logger.debug("cancel1..." + vo.getCode());
			}
		}.executeWithinEtfTcc();
	}

	@EtfTcc(transEnumClazz = TccDemoEnum.class, transEnumValue = "step2")
	public void tccStep2(TccDemoVo vo) {
		new EtfTccTransTemplate<TccDemoEnum>(etfTccDaoRedis) {

			@Override
			protected String calcTccTransBizId() {
				return vo.getCode();
			}

			@Override
			protected void tccTry() {
				logger.debug("try2..." + vo.getCode());
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				//throw new RuntimeException("step2 try 失败");
			}

			@Override
			protected void tccConfirm() {
				logger.debug("confirm2..." + vo.getCode());
			}

			@Override
			protected void tccCancel() {
				logger.debug("cancel2..." + vo.getCode());
			}
		}.executeWithinEtfTcc();
	}

}