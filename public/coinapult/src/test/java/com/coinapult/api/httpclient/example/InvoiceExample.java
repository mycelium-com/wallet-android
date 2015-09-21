package com.coinapult.api.httpclient.example;

import com.coinapult.api.httpclient.CoinapultClient;
import com.coinapult.api.httpclient.Transaction;

public class InvoiceExample {
	public static void main(String[] args) {
		CoinapultClient cli =/* new CoinapultClient(
				"b24269410061e0594263f311118ed5",
				"33cb6adea3e5920af43034c8936e48c2196b3a2e76832247e3deae804ad7");*/
      null;
		try {
			Transaction.Json invoice = cli.receive(1, "BTC", "USD", null);
			System.out.println(invoice.toPrettyString());
			System.out.println(invoice.getExpiration());
			System.out.println(invoice.address);
			System.out.println(invoice.in.expected);
			System.out.println(invoice.out.expected);
		} catch (Throwable err) {
			err.printStackTrace();
		}
		System.exit(0);
	}
}
