package com.coinapult.api.httpclient;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class Address {
	public static class Json extends GenericJson {
		@Key
		public String address;
	}
}
