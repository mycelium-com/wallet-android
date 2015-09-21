package com.coinapult.api.httpclient.example;

import java.util.HashMap;
import java.util.Map;

import com.coinapult.api.httpclient.CoinapultClient;
import com.coinapult.api.httpclient.SearchMany;
import com.coinapult.api.httpclient.Transaction;

public class SearchExample {
	public static void main(String[] args) {
		CoinapultClient cli = /*new CoinapultClient(
				"b24269410061e0594263f311118ed5",
				"33cb6adea3e5920af43034c8936e48c2196b3a2e76832247e3deae804ad7");*/
      null;
		Map<String, String> criteria = new HashMap<String, String>();
		criteria.put("situation", "ok");
		try {
			Transaction.Json res = cli.search(new HashMap<String, String>(
					criteria));
			System.out.println(res.address);

			SearchMany.Json many = cli.searchMany(criteria, 1);
			System.out.println(many.page);
			System.out.println(many.pageCount);
			System.out.println(many.result.get(1).address);
		} catch (Throwable err) {
			err.printStackTrace();
		}
		System.exit(0);
	}
}
