package com.ledger.wallet.bridge.client;

import android.content.Intent;

import com.ledger.wallet.bridge.common.LedgerWalletBridgeConstants;

public class LedgerWalletBridge implements LedgerWalletBridgeConstants {
	
	public static final int SPID = 42;
	//public static final int SPID = 31732824;
	
	public static final int TEST_SPID = 42;
	
    public static Intent open(Integer spid, byte[] ta) {
    	Intent intent = new Intent(INTENT_NAME);
    	intent.setType(MIME_OPEN);
    	intent.putExtra(EXTRA_SPID, spid);
    	intent.putExtra(EXTRA_DATA, ta);
    	return intent;
    }
    public static Intent open() {
    	//return open(null, null);
    	return open(SPID, null);
    }

    public static Intent initStorage(byte[] sessionBlob, byte[] storage) {
    	Intent intent = new Intent(INTENT_NAME);
    	intent.setType(MIME_INIT_STORAGE);
    	intent.putExtra(EXTRA_SESSION, sessionBlob);
    	intent.putExtra(EXTRA_STORAGE, storage);
    	return intent;
    }
    public static Intent initStorage(byte[] sessionBlob) {
    	return initStorage(sessionBlob, null);
    }
    
    public static Intent initNFC(byte[] ta, byte[] storage, String callbackIntent) {
    	Intent intent = new Intent(INTENT_NAME);
    	intent.setType(MIME_LOAD_DEFAULT_APP);
    	intent.putExtra(EXTRA_DATA, ta);
    	intent.putExtra(EXTRA_STORAGE, storage);
    	intent.putExtra(EXTRA_INTENT, callbackIntent);
    	return intent;
    }
    public static Intent initNFC(byte[] ta) {
    	return initNFC(ta, null, null);
    }
    public static Intent initNFC(byte[] ta, byte[] storage) {
    	return initNFC(ta, storage, null);
    }    
    public static Intent initNFC(byte[] ta, String callbackIntent) {
    	return initNFC(ta, null, callbackIntent);
    }    
        
    public static Intent requestStorage(byte[] sessionBlob) {
    	Intent intent = new Intent(INTENT_NAME);
    	intent.setType(MIME_GET_STORAGE);
    	intent.putExtra(EXTRA_SESSION, sessionBlob);
    	return intent;
    }    
    
    public static Intent exchange(byte[] sessionBlob, byte[] request) {
    	Intent intent = new Intent(INTENT_NAME);
    	intent.setType(MIME_EXCHANGE);
    	intent.putExtra(EXTRA_SESSION, sessionBlob);
    	intent.putExtra(EXTRA_DATA, request);
    	return intent;
    }
    
    public static Intent exchange(byte[] sessionBlob, byte protocol, byte[] request, byte[] extendedRequest) {
    	Intent intent = new Intent(INTENT_NAME);
    	intent.setType(MIME_EXCHANGE_EXTENDED);
    	intent.putExtra(EXTRA_SESSION, sessionBlob);
    	intent.putExtra(EXTRA_PROTOCOL, protocol);
    	intent.putExtra(EXTRA_DATA, request);
    	intent.putExtra(EXTRA_EXTENDED_DATA, extendedRequest);
    	return intent;
    }
    
    public static Intent exchange(byte[] sessionBlob, byte protocol, byte[] request, String extendedRequestDataPath) {
    	Intent intent = new Intent(INTENT_NAME);
    	intent.setType(MIME_EXCHANGE_EXTENDED);
    	intent.putExtra(EXTRA_SESSION, sessionBlob);
    	intent.putExtra(EXTRA_PROTOCOL, protocol);
    	intent.putExtra(EXTRA_DATA, request);
    	intent.putExtra(EXTRA_EXTENDED_DATA_PATH, extendedRequestDataPath);
    	return intent;
    }    
        
    public static Intent close(byte[] sessionBlob) {
    	Intent intent = new Intent(INTENT_NAME);
    	intent.setType(MIME_CLOSE);
    	intent.putExtra(EXTRA_SESSION, sessionBlob);
    	return intent;
    }
    
    public static byte[] getSession(Intent intent) {
    	if (intent.getExtras() == null) {
    		return null;
    	}    	
    	return intent.getExtras().getByteArray(EXTRA_SESSION);
    }
	
    public static byte[] getStorage(Intent intent) {
    	if (intent.getExtras() == null) {
    		return null;
    	}    	
    	return intent.getExtras().getByteArray(EXTRA_STORAGE);
    }
    
    public static byte[] getData(Intent intent) {
    	if (intent.getExtras() == null) {
    		return null;
    	}
    	return intent.getExtras().getByteArray(EXTRA_DATA);
    }
    
    public static byte[] getExtendedData(Intent intent) {
    	if (intent.getExtras() == null) {
    		return null;
    	}
    	return intent.getExtras().getByteArray(EXTRA_EXTENDED_DATA);
    }
    
    public static Exception getException(Intent intent) {
    	if (intent.getExtras() == null) {
    		return null;
    	}    	
    	return (Exception)intent.getExtras().get(EXTRA_EXCEPTION);
    }
    
    public static boolean hasException(Intent intent) {
    	if (intent.getExtras() == null) {
    		return true;
    	}
    	return (intent.getExtras().get(EXTRA_EXCEPTION) != null);
    }

}
