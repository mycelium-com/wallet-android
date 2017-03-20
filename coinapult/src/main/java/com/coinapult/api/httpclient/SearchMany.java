package com.coinapult.api.httpclient;

import java.util.List;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class SearchMany {
	public static class Json extends GenericJson {
		@Key
		public List<Transaction.Json> result;

		@Key
		public int page;

		@Key
		public int pageCount;
	}
}
