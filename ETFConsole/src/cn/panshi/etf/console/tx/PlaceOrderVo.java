package cn.panshi.etf.console.tx;

import java.util.Date;

public class PlaceOrderVo {
	String orderId;
	String prdCode;
	int amount;
	Date diliverTime;

	public PlaceOrderVo() {

	}

	public PlaceOrderVo(String orderId, String prdCode, int amount, Date diliverTime) {
		super();
		this.orderId = orderId;
		this.prdCode = prdCode;
		this.amount = amount;
		this.diliverTime = diliverTime;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getPrdCode() {
		return prdCode;
	}

	public void setPrdCode(String prdCode) {
		this.prdCode = prdCode;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public Date getDiliverTime() {
		return diliverTime;
	}

	public void setDiliverTime(Date diliverTime) {
		this.diliverTime = diliverTime;
	}

}