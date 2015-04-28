package com.ledger.wallet.bridge.common;

public interface LedgerWalletBridgeConstants {
	
	public static final String INTENT_NAME = "com.ledger.wallet.intent.WALLET_COMMAND";
	
	public static final String MIME_OPEN = "ledger/open";
	public static final String MIME_INIT_STORAGE = "ledger/initStorage";
	public static final String MIME_GET_STORAGE = "ledger/getStorage";
	public static final String MIME_EXCHANGE = "ledger/exchange";
	public static final String MIME_EXCHANGE_EXTENDED = "ledger/exchangeExtended";
	public static final String MIME_CLOSE = "ledger/close";
	public static final String MIME_LOAD_DEFAULT_APP = "ledger/load";
	
	public static final String EXTRA_SESSION = "ledger_session";
	public static final String EXTRA_STORAGE = "ledger_storage";
	public static final String EXTRA_DATA = "ledger_data";
	public static final String EXTRA_SPID = "ledger_spid";
	public static final String EXTRA_PROTOCOL = "ledger_protocol";
	public static final String EXTRA_EXTENDED_DATA = "ledger_extended_data";
	public static final String EXTRA_EXTENDED_DATA_PATH = "ledger_extended_data_path";
	public static final String EXTRA_EXCEPTION = "ledger_exception";
	public static final String EXTRA_INTENT = "ledger_intent";
}
