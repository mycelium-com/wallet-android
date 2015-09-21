package com.coinapult.api.httpclient;

public class CoinapultError {

	public static class CoinapultException extends Exception {
		private static final long serialVersionUID = -5696705076981149480L;

		public CoinapultException(String message) {
			super(message);
		}
	}

	public static class CoinapultExceptionECC extends CoinapultException {
		private static final long serialVersionUID = 4251558118147850780L;

		public CoinapultExceptionECC(String message) {
			super(message);
		}
	}
}
