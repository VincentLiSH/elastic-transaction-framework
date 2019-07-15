package cn.panshi.etf.tcc;

import java.util.List;

public interface EtfTccDao {

	boolean addEtfTccTransPrepareList(String name, String bizId, String tccEnumValue);

	List<String> findTccTransList2Start(String name, String tccTransBizId);

	void startTccTransByPreparedKey(String key);

}
