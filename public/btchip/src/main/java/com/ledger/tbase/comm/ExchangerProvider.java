package com.ledger.tbase.comm;

import java.util.concurrent.Exchanger;

public class ExchangerProvider {
	
	private static Exchanger<byte[]> exchanger;	
	
	public static Exchanger<byte[]> getNewExchanger() {
		exchanger = new Exchanger<byte[]>();
		return exchanger;
	}
	
	public static Exchanger<byte[]> getExchanger() {
		return exchanger;
	}

}
