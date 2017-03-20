package com.coinapult.api.httpclient;

import java.sql.Timestamp;
import java.util.List;

import com.coinapult.api.httpclient.Ticker.BidAsk;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class TickerHistory {
	public static class Url extends GenericUrl {
		public Url(String encodedUrl) {
			super(encodedUrl);
		}

		@Key
		private long begin;

		@Key
		private long end;

		@Key
		private String market;

		public Url setBegin(long beginTimestamp) {
			begin = beginTimestamp;
			return this;
		}

		public Url setEnd(long endTimestamp) {
			end = endTimestamp;
			return this;
		}

		public Url setMarket(String marketPair) {
			market = marketPair;
			return this;
		}
	}

	public static class Json extends GenericJson {
		@Key
		public List<Item> result;
	}

	public static class Item extends BidAsk {
		@Key
		public long updatetime;

		public Timestamp getUpdatetime() {
			return new Timestamp(updatetime * 1000);
		}
	}
}