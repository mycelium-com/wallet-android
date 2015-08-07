package com.coinapult.api.httpclient;

import java.math.BigDecimal;
import java.sql.Timestamp;

import com.coinapult.api.httpclient.Ticker.BidAsk;
import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class Transaction {
	public static class Json extends GenericJson {
		@Key
		public String type;

		@Key("transaction_id")
		public String tid;

		/* This might be null. */
		@Key
		public String address;

		/* This will be null unless the transaction has completed. */
		@Key
		public Long completeTime;

		public Timestamp getCompleteTime() {
			return new Timestamp(completeTime * 1000);
		}

		/* This might be null. */
		@Key
		public Long expiration;

		public Timestamp getExpiration() {
			return new Timestamp(expiration * 1000);
		}

		@Key
		public Long timestamp;

		public Timestamp getTimestamp() {
			return new Timestamp(timestamp * 1000);
		}

		@Key
		public Half in;

		@Key
		public Half out;

		/* This might be null. */
		@Key
		public BidAsk quote;

		@Key
		public String state;

		/* This might be null. */
		@Key
		public String extOID;
	}

	public static class Half {
		@Key
		public BigDecimal amount;

		@Key
		public String currency;

		@Key
		public BigDecimal expected;
	}
}
