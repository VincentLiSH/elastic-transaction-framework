package cn.panshi.etf.console.ctrl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.zkoss.bind.annotation.AfterCompose;
import org.zkoss.bind.annotation.ContextParam;
import org.zkoss.bind.annotation.ContextType;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.OpenEvent;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Detail;
import org.zkoss.zul.Group;
import org.zkoss.zul.Include;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import cn.panshi.etf4j.tcc.EtfTccDaoRedis;
import cn.panshi.etf4j.tcc.EtfTccStep;

@SuppressWarnings({ "unchecked", "rawtypes" })
@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class TccTxListCtrl {
	@WireVariable
	RedisTemplate redisTemplate;

	@Wire
	Rows rows;

	@AfterCompose(superclass = true)
	public void afterCompose(@ContextParam(ContextType.VIEW) Component view) {
		Selectors.wireComponents(view, this, false);
		Selectors.wireEventListeners(view, this);

		this.renderList();
	}

	private void renderList() {
		Set keys = redisTemplate.keys(EtfTccDaoRedis.ETF_TCC_KEYS.ETF_TCC_SUCCESS_LIST + "*");
		for (Object key : keys) {
			SimpleDateFormat sdf = new SimpleDateFormat(":yyyy:MM:dd_:HH:mm");
			Date keyDate;
			try {
				keyDate = sdf.parse(
						key.toString().substring(EtfTccDaoRedis.ETF_TCC_KEYS.ETF_TCC_SUCCESS_LIST.toString().length()));
			} catch (ParseException e) {
				e.printStackTrace();
				continue;
			}
			String groupStr = new SimpleDateFormat("yyyy/MM/dd HH:mm").format(keyDate);
			Group group = new Group(groupStr);
			group.setAlign("left");
			rows.getChildren().add(group);
			List list = redisTemplate.opsForList().range(key, 0, -1);
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

}