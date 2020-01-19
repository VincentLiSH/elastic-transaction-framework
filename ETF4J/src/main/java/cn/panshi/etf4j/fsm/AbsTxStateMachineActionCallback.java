package cn.panshi.etf4j.fsm;

public abstract class AbsTxStateMachineActionCallback {

	abstract boolean actionReady(String key);

	abstract void doAction(String key);
}