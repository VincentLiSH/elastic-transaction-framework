package cn.panshi.etf.console.tx;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import cn.panshi.etf4j.tcc.EtfTcc;
import cn.panshi.etf4j.tcc.EtfTccDaoRedis;
import cn.panshi.etf4j.tcc.EtfTccTransTemplate;

@Component
public class PlaceOrderTx {
	static Logger logger = Logger.getLogger(PlaceOrderTx.class);
	@Resource
	EtfTccDaoRedis etfTccDaoRedis;

	public enum TCC_PlaceOrderTx {
		store_process, diliver_process;
	}

	@EtfTcc(transEnumClazz = TCC_PlaceOrderTx.class, transEnumValue = "store_process")
	public void storeProcess(final PlaceOrderVo vo) {
		new EtfTccTransTemplate<TCC_PlaceOrderTx>(etfTccDaoRedis) {

			@Override
			protected String calcTccTransBizId() {
				return vo.getOrderId();
			}

			@Override
			protected void tccCancel() {
				logger.info("恢复订单" + vo.getOrderId() + "对应的库存：" + vo.getPrdCode() + " " + vo.getAmount());
			}

			@Override
			protected void tccConfirm() {
				logger.info("扣除订单" + vo.getOrderId() + "对应的库存：" + vo.getPrdCode() + " " + vo.getAmount());
			}

			@Override
			protected void tccTry() {
				logger.info("锁定订单" + vo.getOrderId() + "对应的库存：" + vo.getPrdCode() + " " + vo.getAmount());
				if (Math.random() > 0.9) {
					throw new RuntimeException("30%概率随机出错");
				}
			}

		}.executeWithinEtfTcc();
	}

	@EtfTcc(transEnumClazz = TCC_PlaceOrderTx.class, transEnumValue = "diliver_process")
	public void diliver(final PlaceOrderVo vo) {
		new EtfTccTransTemplate<TCC_PlaceOrderTx>(etfTccDaoRedis) {

			@Override
			protected String calcTccTransBizId() {
				return vo.getOrderId();
			}

			@Override
			protected void tccCancel() {
				logger.info("撤销订单" + vo.getOrderId() + "对应的物流单：" + vo.getPrdCode() + " " + vo.getAmount());
			}

			@Override
			protected void tccConfirm() {
				logger.info("订单" + vo.getOrderId() + "对应的物流下单：" + vo.getPrdCode() + " " + vo.getAmount());
			}

			@Override
			protected void tccTry() {
				logger.info("准备订单" + vo.getOrderId() + "物流资源：" + vo.getPrdCode() + " " + vo.getAmount() + " "
						+ vo.getDiliverTime());
				if (Math.random() > 0.9) {
					throw new RuntimeException("30%概率随机出错");
				}
			}

		}.executeWithinEtfTcc();
	}
}