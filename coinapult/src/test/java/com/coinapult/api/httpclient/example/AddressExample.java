package com.coinapult.api.httpclient.example;

import com.coinapult.api.httpclient.*;

public class AddressExample {
	public static void main(String[] args) {
		CoinapultClient cli = null; /* new CoinapultClient(
				"b24269410061e0594263f311118ed5",
				"33cb6adea3e5920af43034c8936e48c2196b3a2e76832247e3deae804ad7",new CoinapultProdConfig());*/
		try {
			AccountInfo.Json resA = cli.accountInfo();
			System.out.println(resA.toPrettyString());
			System.out.println(resA.role);
			System.out.println(resA.balances.size());
			System.out.println(resA.balances.get(0).currency);
			System.out.println(resA.balances.get(0).amount);
			Address.Json res = cli.getBitcoinAddress();
			System.out.println(res.toPrettyString());
			System.out.println(res.address);
		} catch (Throwable err) {
			err.printStackTrace();
		}

		try {
			AddressInfo.Json res = cli.accountAddress("w@mailinator.com");
			System.out.println(res.toPrettyString());
			System.out.println(res.status);
			System.out.println(res.address);
		} catch (Throwable err) {
			err.printStackTrace();
		}
		System.exit(0);
	}
}
