package com.coinapult.api.httpclient.example;

import com.coinapult.api.httpclient.CoinapultClient;
import com.coinapult.api.httpclient.Ticker;
import com.coinapult.api.httpclient.TickerHistory;

public class TickerExample {
	public static void main(String[] args) {
		CoinapultClient cli = null;
		Ticker.Json t;
		try {
			t = cli.ticker();
			System.out.println(t.toPrettyString());
			System.out.println("index: " + t.index);
			System.out.println(t.type);
			System.out.println(t.updatetime);
			System.out.println(t.getUpdatetime());
			System.out.println(t.market);
		} catch (Throwable err) {
			err.printStackTrace();
		}

		TickerHistory.Json t2;
		long now = System.currentTimeMillis() / 1000L;
		try {
			t2 = cli.tickerHistory(now - (60 * 10), now);
			System.out.println(t2.toPrettyString());
			for (TickerHistory.Item item : t2.result) {
				System.out.println(item.getUpdatetime());
				System.out.println(item.ask);
				System.out.println(item.bid);
				System.out.println("======");
			}
		} catch (Throwable err) {
			err.printStackTrace();
		}
		System.exit(0);
	}
}
