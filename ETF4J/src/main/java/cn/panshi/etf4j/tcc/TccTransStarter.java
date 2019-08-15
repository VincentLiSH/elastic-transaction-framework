package cn.panshi.etf4j.tcc;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSONObject;

/**
 * @author 李英权 <49069554@qq.com>
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class TccTransStarter<T_tcc_trans_enum_type extends Enum<T_tcc_trans_enum_type>> {
	private static Logger logger = Logger.getLogger(TccTransStarter.class);

	private final static ThreadLocal<Class<? extends Enum>> TCC_ENUM_TYPE_OF_JUST_PREPARED_TRANS = new ThreadLocal<>();
	private final static ThreadLocal<String> TCC_ENUM_VALUE_OF_JUST_PREPARED_TRANS = new ThreadLocal<>();
	private final static ThreadLocal<JSONObject> TCC_CURR_INPUT_PARAM = new ThreadLocal<>();

	private EtfTccDao etfTccDao;

	private String tccTransBizId = null;

	Class<? extends Enum<T_tcc_trans_enum_type>> tccTransEnumClass;

	Set<String> tccTransStepSet = new HashSet<>();

	public TccTransStarter(EtfTccDao etfTccDao) {
		super();
		this.etfTccDao = etfTccDao;
	}

	/**
	 * 对ps所包裹的TCC交易步骤做一系列执行前的准备工作：
	 * 1 检查其交易类型合法性（enum value合法性、与其它步骤是否存在冲突、重复等）；
	 * 2 获取其对应的bizId交易流水号；
	 * 3 获取该TCC交易步骤的调用入参；
	 * 4 将此交易步骤调用4要素（_tccEnumClass.getName(), _bizId, _tccEnumStringValue,inputParam）存入redis，用于后续的启动和交易回调；
	 * 
	 * @param ps 相当于一个闭包——包裹了对1个TCC交易步骤的调用
	 */
	public final void prepareTccTrans(TccTransPrepareStatement ps) throws EtfTccException4PrepareStage {
		try {
			EtfTccAop.setTccCurrStagePrepare();

			String _bizId = invokePrepare2GetBizId(ps);

			Class<? extends Enum> _tccEnumClass = TCC_ENUM_TYPE_OF_JUST_PREPARED_TRANS.get();
			String _tccEnumStringValue = TCC_ENUM_VALUE_OF_JUST_PREPARED_TRANS.get();

			if (_tccEnumClass == null || _tccEnumStringValue == null) {
				throw new EtfTccException4PrepareStage("未检测到交易类型上下文，请确保TCC交易API正确配置了@EtfTcc");
			}

			T_tcc_trans_enum_type _tccEnumValue = null;
			try {
				_tccEnumValue = (T_tcc_trans_enum_type) Enum.valueOf(_tccEnumClass, _tccEnumStringValue);
			} catch (Exception e) {
				throw new EtfTccException4PrepareStage("准备启动的事务类型[" + _tccEnumStringValue + "]不是一个合法的"
						+ _tccEnumClass.getName() + "枚举值，请确保TCC交易API正确配置了@EtfTcc！");
			}

			if (tccTransEnumClass == null) {
				tccTransEnumClass = (Class<? extends Enum<T_tcc_trans_enum_type>>) _tccEnumValue.getClass();
			}

			if (!tccTransEnumClass.equals(_tccEnumValue.getClass())) {
				throw new EtfTccException4PrepareStage("Tcc事务启动器的交易类型[" + tccTransEnumClass.getName() + "]与准备启动的事务类型["
						+ _tccEnumValue.getClass().getName() + "]不符，请确保TccTransStarter准备的多个TCC交易类型一致！");
			}

			if (StringUtils.isBlank(_bizId)) {
				throw new EtfTccException4PrepareStage("TCC交易[" + _tccEnumClass.getName() + "." + _tccEnumStringValue
						+ "]准备阶段返回了空的bizId业务流水号，请确保正确实现了TCC交易回调！");
			}
			if (tccTransBizId == null) {
				tccTransBizId = _bizId;
			} else {
				if (!StringUtils.equals(tccTransBizId, _bizId)) {
					throw new EtfTccException4PrepareStage(
							"TCC交易[" + _tccEnumClass.getName() + "." + _tccEnumStringValue + "]准备阶段返回bizId业务流水号"
									+ _bizId + "与前面交易准备返回的[" + tccTransBizId + "]不一致，请确保正确实现了TCC交易回调！");
				}
			}

			if (tccTransStepSet.contains(_tccEnumStringValue)) {
				throw new EtfTccException4PrepareStage("TCC交易[" + _tccEnumClass.getName() + "." + _tccEnumStringValue
						+ "]准备失败，请确保TccTransStarter准备的多个TCC交易不存在重复类型！");
			} else {
				tccTransStepSet.add(_tccEnumStringValue);
				if (tccTransStepSet.size() == 1) {
					etfTccDao.initTccCounter4Try(_tccEnumClass.getName(), _bizId);
				}
			}

			JSONObject inputParam = TCC_CURR_INPUT_PARAM.get();
			etfTccDao.saveNewEtfTccStep(_tccEnumClass.getName(), _bizId, _tccEnumStringValue,
					inputParam == null ? null : inputParam.toJSONString());

			logger.debug("TCC交易[" + _tccEnumClass.getName() + "." + _tccEnumStringValue + "]准备成功");
		} finally {
			TCC_ENUM_TYPE_OF_JUST_PREPARED_TRANS.remove();
			TCC_ENUM_VALUE_OF_JUST_PREPARED_TRANS.remove();
			TCC_CURR_INPUT_PARAM.remove();
		}
	}

	protected String invokePrepare2GetBizId(TccTransPrepareStatement ps) {
		try {
			ps.doPrepare();//会被EtfTccAop拦截 设置TCC交易类型上下文到ThreadLocal，供下面的代码做交易类型检查
		} catch (EtfTccException4ReturnBizCode e) { //EtfTccAop抛出此异常 隐式的返回bizId，避免要求子类显式的返回  造成框架侵入性过高和使用不便！
			return e.getBizId();
		}
		return null;
	}

	/**
	 * memo:并发异步执行前面prepareTccTrans准备的所有TCC交易
	 */
	public final void startTccTransList() throws EtfTccException4StartStage {
		if (tccTransEnumClass == null) {
			throw new EtfTccException4StartStage("TCC交易无法启动，请检查交易准备逻辑！");
		}
		Enum[] enumConstants = tccTransEnumClass.getEnumConstants();
		if (enumConstants.length != tccTransStepSet.size()) {
			throw new EtfTccException4StartStage("TCC交易个数[" + tccTransStepSet.size() + "]与"
					+ tccTransEnumClass.getName() + "定义[" + enumConstants.length + "]不一致！");
		}

		for (Enum tccTransEnum : enumConstants) {
			etfTccDao.startTccTransAsynch(tccTransEnumClass.getName(), tccTransEnum.name(), tccTransBizId);
		}
	}

	public static void setTCC_ENUM_TYPE_OF_JUST_PREPARED_TRANS(Class<? extends Enum> transEnumClazz)
			throws InstantiationException, IllegalAccessException {
		TCC_ENUM_TYPE_OF_JUST_PREPARED_TRANS.set(transEnumClazz);

	}

	public static void setTCC_ENUM_VALUE_OF_JUST_PREPARED_TRANS(String transEnumValue) {
		TCC_ENUM_VALUE_OF_JUST_PREPARED_TRANS.set(transEnumValue);
	}

	public static void setTCC_CURR_INPUT_PARAM(JSONObject inputParamJSON) {
		TCC_CURR_INPUT_PARAM.set(inputParamJSON);
	}

}