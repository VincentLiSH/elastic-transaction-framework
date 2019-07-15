package cn.panshi.etf.tcc;

<<<<<<< HEAD
import java.util.List;

public interface EtfTccDao {

	boolean addEtfTccTransPrepareList(String name, String bizId, String tccEnumValue);

	List<String> findTccTransList2Start(String name, String tccTransBizId);

	void startTccTransByPreparedKey(String key);
=======
public interface EtfTccDao {

	void addEtfTccTransPrepareList(String name, String bizId, String tccEnumValue);
>>>>>>> branch 'dev' of https://github.com/VincentLiSH/elastic-transaction-framework.git

}
