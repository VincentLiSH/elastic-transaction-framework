package cn.panshi.etf.console.ctrl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.zkoss.bind.annotation.AfterCompose;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ContextParam;
import org.zkoss.bind.annotation.ContextType;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.DesktopUnavailableException;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.OpenEvent;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Detail;
import org.zkoss.zul.Group;
import org.zkoss.zul.Include;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tab;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import cn.panshi.etf.console.tx.PlaceOrderTx;
import cn.panshi.etf.console.tx.PlaceOrderTx.TCC_PlaceOrderTx;
import cn.panshi.etf.console.tx.PlaceOrderVo;
import cn.panshi.etf.console.tx.RedisMessageListener;
import cn.panshi.etf.console.tx.RedisMessageListenerCallback;
import cn.panshi.etf4j.tcc.EtfTccDaoRedis;
import cn.panshi.etf4j.tcc.EtfTccException4PrepareStage;
import cn.panshi.etf4j.tcc.EtfTccException4StartStage;
import cn.panshi.etf4j.tcc.EtfTccStep;
import cn.panshi.etf4j.tcc.TccTransPrepareStatement;
import cn.panshi.etf4j.tcc.TccTransStarter;

@SuppressWarnings({ "unchecked", "rawtypes" })
@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class TccTxListCtrl {
	private final Logger logger = Logger.getLogger(getClass());

	@WireVariable
	RedisTemplate redisTemplate;
	@WireVariable
	JedisConnectionFactory jedisConnectionFactory;
	@WireVariable
	RedisMessageListener redisMessageListener;
	@WireVariable
	PlaceOrderTx placeOrderTx;
	@WireVariable
	EtfTccDaoRedis etfTccDaoRedis;
	@Wire
	Rows rowsSuccess;
	@Wire
	Rows rowsFailure;
	@Wire
	Tab tabCanceled;
	@Wire
	Tab tabSuccess;

	Desktop desktop;
	RedisMessageListenerCallback callback = new RedisMessageListenerCallback() {

		@Override
		public void callBack(String msgChannel, String msgBody) {
			try {
				Executions.activate(desktop);
				if (EtfTccDaoRedis.ETF_TCC_PUB_CHANNEL.TCC_SUCCESS_NOTIFY.toString().equals(msgChannel)) {
					Clients.showNotification("一个交易成功", "info", tabSuccess, "after_center", 2000, false);
					renderList(rowsSuccess, EtfTccDaoRedis.ETF_TCC_KEYS.ETF_TCC_SUCCESS_LIST.toString(),
							":yyyy:MM:dd_:HH:mm");
				} else {
					Clients.showNotification("一个交易失败撤销", "warning", tabCanceled, "after_center", 2000, false);
					renderList(rowsFailure, EtfTccDaoRedis.ETF_TCC_KEYS.ETF_TCC_CANCELED_LIST.toString(),
							":yyyy:MM:dd_:HH");
				}
				redisMessageListener.addCallback(callback);
			} catch (DesktopUnavailableException | InterruptedException e) {
				e.printStackTrace();
			}

			Executions.deactivate(desktop);
		}
	};

	@AfterCompose(superclass = true)
	public void afterCompose(@ContextParam(ContextType.VIEW) Component view)
			throws DesktopUnavailableException, InterruptedException {
		Selectors.wireComponents(view, this, false);
		Selectors.wireEventListeners(view, this);

		desktop = Executions.getCurrent().getDesktop();
		desktop.enableServerPush(true);
		//		Executions.activate(Executions.getCurrent().getDesktop());

		this.renderList(rowsSuccess, EtfTccDaoRedis.ETF_TCC_KEYS.ETF_TCC_SUCCESS_LIST.toString(), ":yyyy:MM:dd_:HH:mm");
		this.renderList(rowsFailure, EtfTccDaoRedis.ETF_TCC_KEYS.ETF_TCC_CANCELED_LIST.toString(), ":yyyy:MM:dd_:HH");

		redisMessageListener.addCallback(callback);
	}

	private void renderList(Rows rows, String tccTxList, String txListDateFormat) {
		rows.getChildren().clear();
		Set keys = redisTemplate.keys(tccTxList + "*");
		for (Object key : keys) {
			SimpleDateFormat sdf = new SimpleDateFormat(txListDateFormat);
			Date keyDate;
			try {
				keyDate = sdf.parse(key.toString().substring(tccTxList.length()));
			} catch (ParseException e) {
				e.printStackTrace();
				continue;
			}
			String groupStr = new SimpleDateFormat("yyyy/MM/dd HH:mm").format(keyDate);
			Group group = new Group(groupStr);
			group.setAlign("left");
			rows.getChildren().add(group);
			List list = redisTemplate.opsForList().range(key, 0, -1);

			group.setLabel(group.getLabel() + "[" + list.size() + "]");

			int i = 1;
			for (final Object item : list) {
				Row row = new Row();
				row.setStyle("cursor:hand;cursor:pointer;");

				final Detail detail = new Detail();
				row.getChildren().add(detail);
				detail.addEventListener("onOpen", new EventListener<OpenEvent>() {

					@Override
					public void onEvent(OpenEvent e) throws Exception {
						if (e.isOpen()) {
							Include include = new Include();
							include.setDynamicProperty("stepList", renderTccStepList(item.toString()));
							include.setSrc("/zuls/tccTxStepDetail.zul");
							include.invalidate();
							detail.getChildren().clear();
							detail.getChildren().add(include);
						} else {

						}
					}
				});

				row.getChildren().add(new Label("" + i));
				i++;
				row.getChildren().add(new Label(item.toString()));
				rows.getChildren().add(row);

				/*row.addEventListener("onClick", new EventListener<ClickEvent>() {
				
					@Override
					public void onEvent(ClickEvent arg0) throws Exception {
						renderTccStepList(item.toString());
					}
				
				});*/
			}
		}
	}

	private List<EtfTccStep> renderTccStepList(String tccTxRecordKey) {
		List<EtfTccStep> stepList = new ArrayList<>();
		Set keys = redisTemplate.keys(EtfTccDaoRedis.ETF_TCC_KEYS.ETF_TCC_STEP + ":" + tccTxRecordKey + "*");
		stepList.addAll(redisTemplate.opsForValue().multiGet(keys));
		for (EtfTccStep s : stepList) {
			JSONObject object = JSONObject.parseObject(s.getBizStateJson());
			String jsonFormatStr = JSON.toJSONString(object, SerializerFeature.PrettyFormat,
					SerializerFeature.WriteMapNullValue, SerializerFeature.WriteDateUseDateFormat);
			s.setBizStateJson(jsonFormatStr);
		}
		return stepList;
	}

	@Command
	public void exePlaceOrderTx() throws EtfTccException4PrepareStage, EtfTccException4StartStage {
		final PlaceOrderVo vo = new PlaceOrderVo(UUID.randomUUID().toString(), "prdCode", 1, new Date());

		TccTransStarter<TCC_PlaceOrderTx> starter = new TccTransStarter<TCC_PlaceOrderTx>(etfTccDaoRedis);

		starter.prepareTccTrans(new TccTransPrepareStatement() {
			@Override
			public void doPrepare() {
				placeOrderTx.diliver(vo);
			}
		});

		starter.prepareTccTrans(new TccTransPrepareStatement() {
			@Override
			public void doPrepare() {
				placeOrderTx.storeProcess(vo);
			}
		});

		try {
			starter.startTccTransList();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}