package com.coinapult.api.httpclient;

import java.math.BigDecimal;
import java.sql.Timestamp;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class Ticker {
	public static class Url extends GenericUrl {
		public Url(String encodedUrl) {
			super(encodedUrl);
		}

		@Key
		private String market;

		@Key
		private String filter;

		public Url setMarket(String marketPair) {
			market = marketPair;
			return this;
		}

		/** CSV listing the keys to return. */
		public Url setFilter(String keyFilter) {
			filter = keyFilter;
			return this;
		}
	}

	public static class Json extends GenericJson {
		@Key
		public String type; /* "ticker" */

		@Key
		public BigDecimal index;

		@Key
		public String market;

		@Key
		public long updatetime;

		public Timestamp getUpdatetime() {
			return new Timestamp(updatetime * 1000);
		}

		@Key
		public BidAsk small;

		@Key
		public BidAsk medium;

		@Key
		public BidAsk large;

		@Key
		public BidAsk vip;

		@Key("vip+")
		public BidAsk vip_plus;

		@Key("100")
		public BidAsk bidask_100;

		@Key("500")
		public BidAsk bidask_500;

		@Key("2000")
		public BidAsk bidask_2000;

		@Key("5000")
		public BidAsk bidask_5000;

		@Key("10000")
		public BidAsk bidask_10000;
	}

	public static class BidAsk {
		@Key
		public BigDecimal bid;

		@Key
		public BigDecimal ask;
	}
}