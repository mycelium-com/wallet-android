package com.ledger.tbase.comm;

import java.util.concurrent.TimeUnit;

import com.ledger.wallet.bridge.client.LedgerWalletBridge;
import com.ledger.wallet.bridge.common.LedgerWalletBridgeConstants;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class LedgerTBounceActivity extends Activity implements LedgerWalletBridgeConstants {
	
	private static final String TAG = "LedgerTBounceActivity";
	
	protected static final String EXTRA_EXCHANGE = "ledger_bounce_exchange";
	protected static final String EXTRA_NVM = "ledger_bounce_nvm";
	protected static final String EXTRA_SESSION = "ledger_bounce_session";
	protected static final String EXTRA_DATA = "ledger_bounce_data";
	protected static final String EXTRA_SPID = "ledger_bounce_spid";
	protected static final String EXTRA_PROTOCOL = "ledger_bounce_protocol";
	protected static final String EXTRA_EXTENDED_DATA = "ledger_bounce_extended_data";
	protected static final String EXTRA_EXTENDED_DATA_PATH = "ledger_bounce_extended_data_path";
		
	protected static final String MIME_INIT_INTERNAL = "ledgerbounce/init";
	protected static final String MIME_GET_NVM_INTERNAL = "ledgerbounce/getnvm";
	protected static final String MIME_CLOSE_INTERNAL = "ledgerbounce/close";
	protected static final String MIME_EXCHANGE_INTERNAL = "ledgerbounce/exchange";
	protected static final String MIME_EXCHANGE_EXTENDED_INTERNAL = "ledgerbounce/exchangeExtended";
	
	//protected static final int EXCHANGE_TIMEOUT_MS = 30000;
	protected static final int EXCHANGE_TIMEOUT_MS = 120000;
	
	private static final int REQUEST_CODE = 1;
			
	@Override
    protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "Created");
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) { // coming back on activity result
			return;
		}				
		Intent dispatchIntent = null;
		String intentType = getIntent().getType();
		byte[] nvm = null;
		byte[] session = null;
		byte[] data = null;
		byte protocol = (byte)0;
		byte[] extendedData = null;
		byte[] ta = null;
		String extendedDataPath = null;
		int spid = LedgerWalletBridge.TEST_SPID;
		if (getIntent().getExtras() != null) {
			nvm = getIntent().getExtras().getByteArray(EXTRA_NVM);
			session = getIntent().getExtras().getByteArray(EXTRA_SESSION);
			data = getIntent().getExtras().getByteArray(EXTRA_DATA);
			protocol = getIntent().getExtras().getByte(EXTRA_PROTOCOL, (byte)0);
			extendedData = getIntent().getExtras().getByteArray(EXTRA_EXTENDED_DATA);
			spid = getIntent().getExtras().getInt(EXTRA_SPID, spid);
			extendedDataPath = getIntent().getExtras().getString(EXTRA_EXTENDED_DATA_PATH);
		}		
		if (intentType.equals(MIME_INIT_INTERNAL)) {
			if (data != null) {
				dispatchIntent = LedgerWalletBridge.open(spid, data);
			}
			else {
				dispatchIntent = LedgerWalletBridge.open();
			}
			dispatchIntent.putExtra(EXTRA_NVM, nvm);
		}
		else
		if (intentType.equals(MIME_GET_NVM_INTERNAL)) {
			dispatchIntent = LedgerWalletBridge.requestStorage(session);
		}
		else
		if (intentType.equals(MIME_EXCHANGE_INTERNAL)) {
			dispatchIntent = LedgerWalletBridge.exchange(session, data);
		}		
		else
		if (intentType.equals(MIME_EXCHANGE_EXTENDED_INTERNAL)) {
			if (extendedDataPath == null) {
				dispatchIntent = LedgerWalletBridge.exchange(session, protocol, data, extendedData);
			}
			else {
				dispatchIntent = LedgerWalletBridge.exchange(session, protocol, data, extendedDataPath);
			}
		}
		else
		if (intentType.equals(MIME_CLOSE_INTERNAL)) {
			dispatchIntent = LedgerWalletBridge.close(session);
		}
		if (dispatchIntent != null) {
			Log.d(TAG, "Dispatch request");
			startActivityForResult(dispatchIntent, REQUEST_CODE);			
		}
	}
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	Log.d(TAG, "Got response " + data.getType());
    	if ((resultCode != Activity.RESULT_OK) || (LedgerWalletBridge.hasException(data)) || (data.getExtras() == null)) {
    		Log.d(TAG, "Exception notified");
    		try {
    			ExchangerProvider.getExchanger().exchange(null, EXCHANGE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    		}
    		catch(Exception e) {    			
    		}    		
    	}
    	else {
    		byte[] result = null;
    		byte[] session = LedgerWalletBridge.getSession(data);
    		if (data.getType().equals(MIME_OPEN)) {
    			byte[] nvm = data.getExtras().getByteArray(EXTRA_NVM);
    			Log.d(TAG, "Open NVM : " + (nvm != null));
    			startActivityForResult(LedgerWalletBridge.initStorage(session, nvm), 1);
    			return;
    		}
    		else
    		if (data.getType().equals(MIME_INIT_STORAGE)) {
    			result = session;
    		}
    		else
    		if (data.getType().equals(MIME_EXCHANGE)) {
    			result = LedgerWalletBridge.getData(data);
    		}
    		else
    		if (data.getType().equals(MIME_EXCHANGE_EXTENDED)) {
    			result = LedgerWalletBridge.getData(data);
    			byte[] extendedResult = LedgerWalletBridge.getExtendedData(data);
    		}
    		else
    		if (data.getType().equals(MIME_GET_STORAGE)) {
    			result = LedgerWalletBridge.getStorage(data);
    		}
    		else
    		if (data.getType().equals(MIME_CLOSE)) {
    			result = new byte[0];
    		}
    		try {
    			ExchangerProvider.getExchanger().exchange(result, EXCHANGE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    		}
    		catch(Exception e) {    			
    		}    		    		
    	}
    	finish();
    }
}
