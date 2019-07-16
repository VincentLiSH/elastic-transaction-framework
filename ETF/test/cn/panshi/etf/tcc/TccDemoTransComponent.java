package cn.panshi.etf.tcc;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import cn.panshi.etf.core.EtfException4LockConcurrent;

@Component
public class TccDemoTransComponent {
	static Logger logger = LoggerFactory.getLogger(TccDemoTransComponent.class);
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
					logger.debug("step1 try..." + vo.getCode());
					//					throw new RuntimeException("step1 try 失败");
				}

				@Override
				protected void tccConfirm() {
					logger.debug("confirm1..." + vo.getCode());
				}

				@Override
				protected void tccCancel() {
					logger.debug("cancel1..." + vo.getCode());
				}
			}.executeWithinEtfTcc();
		} catch (EtfException4LockConcurrent e) {
			logger.error(e.getMessage());
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
					logger.debug("try2..." + vo.getCode());
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
		} catch (EtfException4LockConcurrent e) {
			logger.error(e.getMessage());
		}
	}

}