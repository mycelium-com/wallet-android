package com.mycelium.wallet.colu.json;

import java.util.List;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class AddressInfo {
	public static class Json extends GenericJson {
		@Key
		public String address;

		@Key
		public List<Utxo.Json> utxos;

	}
}
