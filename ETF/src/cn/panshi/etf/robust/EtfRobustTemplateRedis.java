package cn.panshi.etf.robust;

public abstract class EtfRobustTemplateRedis<T_etf_trans_type extends Enum<T_etf_trans_type>, T_return>
		extends EtfRobustTemplate<T_etf_trans_type, T_return> {

	public EtfRobustTemplateRedis(EtfRobDaoRedis etfRobDaoRedis) {
		super(etfRobDaoRedis);
	}
}