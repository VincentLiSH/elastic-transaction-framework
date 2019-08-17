package cn.panshi.etf.console.ctrl;

import java.util.List;

import org.zkoss.bind.annotation.ExecutionArgParam;
import org.zkoss.bind.annotation.Init;

import cn.panshi.etf4j.tcc.EtfTccStep;

public class TccTxStepDetailCtrl {
	List<EtfTccStep> stepList;

	@Init
	public void init(@ExecutionArgParam("stepList") List<EtfTccStep> stepList) {
		this.stepList = stepList;
	}

	public List<EtfTccStep> getStepList() {
		return stepList;
	}

}