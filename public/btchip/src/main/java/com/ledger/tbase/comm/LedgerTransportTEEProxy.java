package com.ledger.tbase.comm;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.btchip.BTChipConstants;
import com.btchip.BTChipException;
import com.btchip.comm.BTChipTransport;
import com.btchip.comm.android.BTChipTransportAndroid;
import com.btchip.utils.Dump;
import com.ledger.tbase.utils.LedgerTAUtils;
import com.ledger.wallet.bridge.common.LedgerWalletBridgeConstants;

public class LedgerTransportTEEProxy implements BTChipTransport, LedgerWalletBridgeConstants {
	
	public static final String TAG="LedgerTransportTEEProxy";
	
	private ExecutorService executor;
	private Context context;
	private byte[] session;
	private byte[] nvm;
	private boolean debug;
	
	private static final byte[] APDU_INIT[] = {
		Dump.hexToBin("D020000038000000000000000118F43F95A217EFEDE0A8D98DAC357E3B2501E79C3958B9D7E15238D43A6807C397680EB805BC0E95E2B65D9E49B1B045"),
		Dump.hexToBin("D02200002B000000020000000120B25006C589F0DCF1BBB75BAA1542A5E6CF300995F0046DE59CC641C0798D9D489006")
	};
	
	private static final int SW_OK = 0x9000;
	private static final int SW_CONDITIONS_NOT_SATISFIED = 0x6985;
		
	public LedgerTransportTEEProxy(Context context, ExecutorService executor) {
		this.context = context;
		this.executor = executor;
	}
	
	public LedgerTransportTEEProxy(Context context) {
		this(context, new ScheduledThreadPoolExecutor(1));
	}
	
	public byte[] getNVM() {
		return nvm;
	}
	public void setNVM(byte[] nvm) {
		this.nvm = nvm;
	}
	
	public boolean init() {
		// Quick check, if the main intent cannot be resolved, exit immediately
		Intent intent = new Intent(INTENT_NAME);
		intent.setType(MIME_OPEN);
		if (intent.resolveActivity(context.getPackageManager()) == null) {
			return false;
		}
		Exchanger<byte[]> exchanger = ExchangerProvider.getNewExchanger();
		Intent runIntent = new Intent(context, LedgerTBounceActivity.class);
		runIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		runIntent.setType(LedgerTBounceActivity.MIME_INIT_INTERNAL);
		runIntent.putExtra(LedgerTBounceActivity.EXTRA_NVM, nvm);
		LedgerTAUtils.LedgerTA ta = LedgerTAUtils.getTA();
		if (ta != null) {
			runIntent.putExtra(LedgerTBounceActivity.EXTRA_DATA, ta.getTA());
			runIntent.putExtra(LedgerTBounceActivity.EXTRA_SPID, ta.getSPID());
		}
		context.startActivity(runIntent);				
		try {
			session = exchanger.exchange(new byte[0], LedgerTBounceActivity.EXCHANGE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
			if (session == null) {
				return false;
			}
		}
		catch(Exception e) {
			Log.d(TAG, "Exchanger failed", e);
			return false;
		}
		
		try {
			for (byte[] apdu : APDU_INIT) {
				int sw = 0;
				byte[] response = exchange(apdu).get();
				if ((response != null) && (response.length > 2)) {
					sw = ((response[response.length - 2] & 0xff) << 8) | (response[response.length - 1] & 0xff);
					if ((sw != SW_OK) && (sw != SW_CONDITIONS_NOT_SATISFIED)) {
						throw new BTChipException("Invalid response status " + Integer.toHexString(sw));
					}
				}
			}
		}
		catch(Exception e) {
			try {
				close();
			}
			catch(Exception e1) {				
			}
			Log.d(TAG, "Init failed", e);
			return false;
		}
						
		return true;
	}
	
	private boolean needExternalUI(byte[] commandParam) {
		// Some commands need to call the Trusted UI :
		// SETUP, VERIFY PIN, HASH INPUT FINALIZE, HASH INPUT FINALIZE FULL
		byte ins = commandParam[1];
		switch(ins) {
			case BTChipConstants.BTCHIP_INS_SETUP:
			case BTChipConstants.BTCHIP_INS_VERIFY_PIN:
			case BTChipConstants.BTCHIP_INS_HASH_INPUT_FINALIZE:
			case BTChipConstants.BTCHIP_INS_HASH_INPUT_FINALIZE_FULL:
				return true;
			default:
				return false;
		}
	}

	@Override
	public Future<byte[]> exchange(byte[] commandParam) throws BTChipException {
		if (debug) {
			Log.d(BTChipTransportAndroid.LOG_STRING, "=> " + Dump.dump(commandParam));
		}		
		if (session == null) {
			throw new BTChipException("Session is not open");
		}		
		final Exchanger<byte[]> exchanger = ExchangerProvider.getNewExchanger();
		final byte[] command = commandParam;
		Intent runIntent = new Intent(context, LedgerTBounceActivity.class);
		runIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);		
		runIntent.putExtra(LedgerTBounceActivity.EXTRA_SESSION, session);
		runIntent.putExtra(LedgerTBounceActivity.EXTRA_DATA, command);
		if (needExternalUI(command)) {
			runIntent.setType(LedgerTBounceActivity.MIME_EXCHANGE_EXTENDED_INTERNAL);
			runIntent.putExtra(LedgerTBounceActivity.EXTRA_PROTOCOL, (byte)0x01);
			//runIntent.putExtra(LedgerTBounceActivity.EXTRA_EXTENDED_DATA, LedgerTAUtils.getTAExternalUI());
			runIntent.putExtra(LedgerTBounceActivity.EXTRA_EXTENDED_DATA_PATH, LedgerTAUtils.getTAExternalUIPath());
		}
		else {
			runIntent.setType(LedgerTBounceActivity.MIME_EXCHANGE_INTERNAL);			
		}
		context.startActivity(runIntent);			
		return executor.submit(new Callable<byte[]>() {
			@Override
			public byte[] call() throws Exception {
				try {
					byte[] response = exchanger.exchange(new byte[0], LedgerTBounceActivity.EXCHANGE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
					if (debug && (response != null)) {
						Log.d(BTChipTransportAndroid.LOG_STRING, "<= " + Dump.dump(response));
					}							
					return response; 
				}
				catch(Exception e) {
					Log.d(TAG, "Exchanger failed", e);
					return null;
				}
			}
		});		
	}

	@Override
	public void close() throws BTChipException {
		
		if (session == null) {
			return;
		}

		Exchanger<byte[]> exchanger = ExchangerProvider.getNewExchanger();
		Intent runIntent = new Intent(context, LedgerTBounceActivity.class);
		runIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		runIntent.putExtra(LedgerTBounceActivity.EXTRA_SESSION, session);
		runIntent.setType(LedgerTBounceActivity.MIME_CLOSE_INTERNAL);
		context.startActivity(runIntent);			
		try {
			exchanger.exchange(new byte[0], LedgerTBounceActivity.EXCHANGE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
			session = null;
		}
		catch(Exception e) {
			Log.d(TAG, "Exchanger failed", e);
		}		
		
	}

	@Override
	public void setDebug(boolean debugFlag) {
		this.debug = debugFlag;
	}
	
	public Future<byte[]> requestNVM() throws BTChipException {
		if (session == null) {
			throw new BTChipException("Session is not open");
		}		
		final Exchanger<byte[]> exchanger = ExchangerProvider.getNewExchanger();
		Intent runIntent = new Intent(context, LedgerTBounceActivity.class);
		runIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		runIntent.setType(LedgerTBounceActivity.MIME_GET_NVM_INTERNAL);
		runIntent.putExtra(LedgerTBounceActivity.EXTRA_SESSION, session);
		context.startActivity(runIntent);			
		return executor.submit(new Callable<byte[]>() {
			@Override
			public byte[] call() throws Exception {
				try {
					byte[] nvmResponse = exchanger.exchange(new byte[0], LedgerTBounceActivity.EXCHANGE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
					return nvmResponse; 
				}
				catch(Exception e) {
					Log.d(TAG, "Exchanger failed", e);
					return null;
				}
			}
		});		
	}
	
	public byte[] loadNVM(String nvmFile) {
		try {
			FileInputStream in = context.openFileInput(nvmFile);
			byte[] nvm = new byte[in.available()];
			in.read(nvm);
			return nvm;
		}
		catch(Exception e) {
			Log.d(TAG, "Unable to load NVM", e);
			return null;
		}
	}
	
	public void writeNVM(String nvmFile, byte[] nvm) throws BTChipException {
		try {
			FileOutputStream out = context.openFileOutput(nvmFile, Context.MODE_PRIVATE);
			out.write(nvm);
   		 	out.flush();
   		 	out.close();			
		}
		catch(Exception e) {
			throw new BTChipException("Unable to write NVM", e);
		}
	}

}
