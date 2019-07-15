package cn.panshi.etf.tcc;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSONObject;

/**
 * @author 李英权 <49069554@qq.com>
 * 
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class TccTransStarter<T_tcc_trans_enum_type extends Enum<T_tcc_trans_enum_type>> {
	private static Logger logger = Logger.getLogger(TccTransStarter.class);

	private final static ThreadLocal<Class<? extends Enum>> TCC_ENUM_TYPE_OF_JUST_PREPARED_TRANS = new ThreadLocal<>();
	private final static ThreadLocal<String> TCC_ENUM_VALUE_OF_JUST_PREPARED_TRANS = new ThreadLocal<>();

	private EtfTccDao etfTccDao;

	private String tccTransBizId = null;

	Class<? extends Enum<T_tcc_trans_enum_type>> tccTransEnumClass;

	public TccTransStarter(EtfTccDao etfTccDao) {
		super();
		this.etfTccDao = etfTccDao;
	}

	public final void prepareTccTrans(TccTransPrepareStatement ps) throws EtfTccException4PrepareStage {
		try {
			EtfTccAop.setCurrTccPrepareStage();

			String bizId = invokePrepare2GetBizId(ps);

			Class<? extends Enum> tccEnumType = TCC_ENUM_TYPE_OF_JUST_PREPARED_TRANS.get();
			String tccEnumValue = TCC_ENUM_VALUE_OF_JUST_PREPARED_TRANS.get();
			if (tccEnumType == null || tccEnumValue == null) {
				throw new EtfTccException4PrepareStage("未检测到交易类型上下文，请确保TCC交易API正确配置了@EtfTcc");
			}

			T_tcc_trans_enum_type tccEnumTypeJustPrepared = null;
			try {
				tccEnumTypeJustPrepared = (T_tcc_trans_enum_type) Enum.valueOf(tccEnumType, tccEnumValue);

			} catch (Exception e) {
				throw new EtfTccException4PrepareStage("准备启动的事务类型[" + tccEnumValue + "]不是一个合法的" + tccEnumType.getName()
						+ "枚举值，请确保TCC交易API正确配置了@EtfTcc！");
			}

			if (tccEnumTypeJustPrepared == null) {
				throw new EtfTccException4PrepareStage("准备启动的事务类型[" + tccEnumValue + "]不是一个合法的" + tccEnumType.getName()
						+ "枚举值，请确保TCC交易API正确配置了@EtfTcc！");
			}

			if (tccTransEnumClass == null) {
				tccTransEnumClass = (Class<? extends Enum<T_tcc_trans_enum_type>>) tccEnumTypeJustPrepared.getClass();
			}

			if (!tccEnumType.equals(tccEnumTypeJustPrepared.getClass())) {
				throw new EtfTccException4PrepareStage("Tcc事务启动器的交易类型[" + tccEnumType.getName() + "]与准备启动的事务类型["
						+ tccEnumTypeJustPrepared.getClass().getName() + "]不符，请确保TCC交易API正确配置了@EtfTcc！");
			}

			if (StringUtils.isBlank(bizId)) {
				throw new EtfTccException4PrepareStage(
						"TCC交易[" + tccEnumType.getName() + "." + tccEnumValue + "]准备阶段未返回bizId业务流水号，请确保正确实现了TCC交易回调！");
			}
			if (tccTransBizId == null) {
				tccTransBizId = bizId;
			} else {
				if (!StringUtils.equals(tccTransBizId, bizId)) {
					throw new EtfTccException4PrepareStage("TCC交易[" + tccEnumType.getName() + "." + tccEnumValue
							+ "]准备阶段返回bizId业务流水号" + bizId + "与前面交易准备返回的[" + tccTransBizId + "]不一致，请确保正确实现了TCC交易回调！");
				}
			}
			boolean success = etfTccDao.addEtfTccTransPrepareList(tccEnumType.getName(), bizId, tccEnumValue);
			if (!success) {
				throw new EtfTccException4PrepareStage("TCC交易[" + tccEnumType.getName() + "." + tccEnumValue
						+ "]准备失败，请确保TCC交易API正确配置了@EtfTcc，不存在重复类型！");
			}

			JSONObject inputParam = EtfTccAop.getCURR_INVOKE_INPUT_PARAM();
			etfTccDao.saveEtfTccRecordStep(tccEnumType.getName(), bizId, tccEnumValue,
					inputParam == null ? null : inputParam.toJSONString());

			logger.debug("TCC交易[" + tccEnumType.getName() + "." + tccEnumValue + "]准备成功");
		} finally {
			TCC_ENUM_TYPE_OF_JUST_PREPARED_TRANS.remove();
			TCC_ENUM_VALUE_OF_JUST_PREPARED_TRANS.remove();
		}
	}

	protected String invokePrepare2GetBizId(TccTransPrepareStatement ps) {
		try {
			ps.doPrepare();//会被EtfTccAop拦截 设置TCC交易类型上下文到ThreadLocal，供下面的代码做交易类型检查
		} catch (EtfTccException4ReturnBizCode e) {//EtfTccAop抛出此异常 隐式的返回bizId，避免要求子类显式的返回  造成框架侵入性过高和使用不便！
			return e.getBizId();
		}
		return null;
	}

	public final void startTccTransList() throws EtfTccException4StartStage {
		Set<String> tccTransKeyList = etfTccDao.findTccTransList2Start(tccTransEnumClass.getName(), tccTransBizId);
		Enum[] enumConstants = tccTransEnumClass.getEnumConstants();
		if (enumConstants.length != tccTransKeyList.size()) {
			throw new EtfTccException4StartStage("TCC交易个数[" + tccTransKeyList.size() + "]与"
					+ tccTransEnumClass.getName() + "定义[" + enumConstants.length + "]不一致！");
		}

		for (String tccTransEnumValue : tccTransKeyList) {
			etfTccDao.startTccTransByPreparedKey(tccTransEnumClass.getName(), tccTransEnumValue, tccTransBizId);
		}
	}

	public static void setTCC_ENUM_TYPE_OF_JUST_PREPARED_TRANS(Class<? extends Enum> transEnumClazz)
			throws InstantiationException, IllegalAccessException {
		TCC_ENUM_TYPE_OF_JUST_PREPARED_TRANS.set(transEnumClazz);

	}

	public static void setTCC_ENUM_VALUE_OF_JUST_PREPARED_TRANS(String transEnumValue) {
		TCC_ENUM_VALUE_OF_JUST_PREPARED_TRANS.set(transEnumValue);
	}

}