package cn.panshi.etf.robust;

public abstract class EtfTemplateWithRedisDao<T_etf_trans_type extends Enum<T_etf_trans_type>, T_return>
		extends EtfTemplate<T_etf_trans_type, T_return> {

	public EtfTemplateWithRedisDao(EtfDaoRedis etfDaoRedis) {
		super(etfDaoRedis);
	}
}