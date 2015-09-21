package com.coinapult.api.httpclient;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class EmailAddress {
	public static class Json extends GenericJson {
		@Key
		public String email;
		@Key
		public  boolean verified;
		@Key
		public String error;
	}
}
