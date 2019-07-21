package cn.panshi.etf4j.robust;

public abstract class EtfRobustTxTemplateRedis<T_etf_trans_type extends Enum<T_etf_trans_type>, T_return>
		extends EtfRobustTxTemplate<T_etf_trans_type, T_return> {

	public EtfRobustTxTemplateRedis(EtfRobDaoRedis etfRobDaoRedis) {
		super(etfRobDaoRedis);
	}
}