package com.coinapult.api.httpclient;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class Config {
	public static class Json extends GenericJson {
		@Key
		public String address;
		public String lockTo;
	}
}
