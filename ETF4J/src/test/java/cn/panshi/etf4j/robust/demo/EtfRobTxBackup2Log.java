package cn.panshi.etf4j.robust.demo;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import cn.panshi.etf4j.robust.EtfRobTxBackupInterface;
import cn.panshi.etf4j.robust.EtfRobTxRecord;

@Component
public class EtfRobTxBackup2Log implements EtfRobTxBackupInterface {

	@Override
	public void doBackUp(EtfRobTxRecord tr) {
		System.out.println("交易记录销毁前备份：" + JSONObject.toJSONString(tr));
	}

}