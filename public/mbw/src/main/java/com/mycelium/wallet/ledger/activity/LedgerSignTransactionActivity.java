package com.mycelium.wallet.ledger.activity;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;

import nordpol.android.TagDispatcher;

import com.btchip.BTChipDongle.BTChipOutput;
import com.btchip.BTChipDongle.BTChipOutputKeycard;
import com.btchip.BTChipDongle.UserConfirmation;
import com.btchip.utils.Dump;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.TransactionOutput;
import com.mrd.bitlib.util.CoinUtil;
import com.mycelium.wallet.activity.send.SignTransactionActivity;
import com.mycelium.wallet.activity.util.Pin;
import com.mycelium.wallet.ledger.LedgerManager;
import com.mycelium.wallet.LedgerPin2FADialog;
import com.mycelium.wallet.LedgerPinDialog;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.PinDialog;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wapi.wallet.AccountScanManager;
import com.mycelium.wapi.wallet.AccountScanManager.AccountStatus;
import com.mycelium.wapi.wallet.AccountScanManager.HdKeyNodeWrapper;
import com.mycelium.wapi.wallet.AccountScanManager.Status;
import com.mycelium.wapi.wallet.bip44.Bip44Account;

public class LedgerSignTransactionActivity extends SignTransactionActivity implements LedgerManager.Events {
	
	private final LedgerManager ledgerManager = MbwManager.getInstance(this).getLedgerManager();
	private LinkedBlockingQueue<String> ledgerPinResponse;
	private boolean _showTx;
	private TagDispatcher dispatcher;
	
	private static final int PAUSE_DELAY = 500;
	private static final String MESSAGE_TITLE_ID = "titleId";
	private static final String MESSAGE_ADDRESS = "address";
	private static final String MESSAGE_KEYCARD_INDEXES = "indexes";
	
	   @Override
	   public void onCreate(Bundle savedInstanceState) {
	      super.onCreate(savedInstanceState);

	      // Syncing Queue for the Ledger and UI Thread on PIN-entry
	      ledgerPinResponse = new LinkedBlockingQueue<String>(1);
	      dispatcher = TagDispatcher.get(this, ledgerManager);
	   }

	   @Override
	   protected void setView() {
	      setContentView(R.layout.sign_ledger_transaction_activity);
	   }
	
	   @Override
	   protected void onResume() {
	      super.onResume();
	      // setup the handlers for the Ledger manager to this activity
	      ledgerManager.setEventHandler(this);	      
	      updateUi();
	      dispatcher.enableExclusiveNfc();
	   }

	   @Override
	   protected void onPause() {
	      super.onPause();	      
	      // unregister me as event handler for Ledger
	      ledgerManager.setEventHandler(null);
	      dispatcher.disableExclusiveNfc();
	   }

	   @Override
	   protected void onNewIntent(Intent intent) {
		   dispatcher.interceptIntent(intent);
	   }
	   
	
	   @Override
	   public void onScanError(String errorMsg) {
	      Utils.showSimpleMessageDialog(LedgerSignTransactionActivity.this, errorMsg, new Runnable() {
	               @Override
	               public void run() {
	                  LedgerSignTransactionActivity.this.setResult(RESULT_CANCELED);
	                  // close this activity and let the user try again
	                  LedgerSignTransactionActivity.this.finish();
	               }
	            });

	      // kill the signing task
	      LedgerSignTransactionActivity.this.startSigningTask().cancel(true);
	   }
	   
	   private void updateUi(){
	      if ((ledgerManager.currentState != AccountScanManager.Status.unableToScan) &&
	    	  (ledgerManager.currentState != AccountScanManager.Status.initializing)) {
	         findViewById(R.id.ivConnectLedger).setVisibility(View.GONE);	         
	      } else {
	         findViewById(R.id.ivConnectLedger).setVisibility(View.VISIBLE);
	         ((TextView)findViewById(R.id.tvPluginLedger)).setText(getString(R.string.ledger_please_plug_in));
	         findViewById(R.id.tvPluginLedger).setVisibility(View.VISIBLE);
	      }

	      if (_showTx){
	         findViewById(R.id.ivConnectLedger).setVisibility(View.GONE);
	         findViewById(R.id.llShowTx).setVisibility(View.VISIBLE);

	         ArrayList<String> toAddresses = new ArrayList<String>(1);

	         long totalSending = 0;

	         for (TransactionOutput o : _unsigned.getOutputs()){
	            Address toAddress;
	            toAddress = o.script.getAddress(_mbwManager.getNetwork());
	            Optional<Integer[]> addressId = ((Bip44Account) _account).getAddressId(toAddress);

	            if (! (addressId.isPresent() && addressId.get()[0]==1) ){
	               // this output goes to a foreign address (addressId[0]==1 means its internal change)
	               totalSending += o.value;
	               toAddresses.add(toAddress.toDoubleLineString());
	            }
	         }

	         String toAddress = Joiner.on(",\n").join(toAddresses);
	         String amount = CoinUtil.valueString(totalSending, false) + " BTC";
	         String total = CoinUtil.valueString(totalSending + _unsigned.calculateFee(), false) + " BTC";
	         String fee = CoinUtil.valueString(_unsigned.calculateFee(), false) + " BTC";

	         ((TextView)findViewById(R.id.tvAmount)).setText(amount);
	         ((TextView)findViewById(R.id.tvToAddress)).setText(toAddress);
	         ((TextView)findViewById(R.id.tvFee)).setText(fee);
	         ((TextView)findViewById(R.id.tvTotal)).setText(total);
	      }
	      else {
		      findViewById(R.id.llShowTx).setVisibility(View.GONE);	    	  
	      }

	   }

	@Override
	public void onPassphraseRequest() {
		throw new RuntimeException("Callback not expected here");
	}

	@Override
	public void onStatusChanged(Status state, AccountStatus accountState) {
		if (state.equals(Status.readyToScan)) {
			connectHandler.sendEmptyMessage(0);
		}
		updateUi();		
	}

	@Override
	public void onAccountFound(HdKeyNodeWrapper account) {
		throw new RuntimeException("Callback not expected here");		
	}

	private Message formatMessage(int titleId) {
		Message message = new Message();
		Bundle bundle = new Bundle();
		bundle.putInt(MESSAGE_TITLE_ID, titleId);
		message.setData(bundle);
		return message;
	}
	
	private Message formatMessage(int titleId, String address, byte[] keycardIndexes) {
		Message message = new Message();
		Bundle bundle = new Bundle();
		bundle.putInt(MESSAGE_TITLE_ID, titleId);
		bundle.putString(MESSAGE_ADDRESS, address);
		bundle.putByteArray(MESSAGE_KEYCARD_INDEXES, keycardIndexes);
		message.setData(bundle);
		return message;
	}

	   final Handler ledgerPinHandler = new Handler(new Handler.Callback() {
		      @Override
		      public boolean handleMessage(Message message) {
		         LedgerPinDialog pin = new LedgerPinDialog(LedgerSignTransactionActivity.this, true);
		         pin.setTitle(message.getData().getInt(MESSAGE_TITLE_ID));
		         pin.setOnPinValid(new PinDialog.OnPinEntered(){
		            @Override
		            public void pinEntered(PinDialog dialog, Pin pin) {
		               ledgerPinResponse.add(pin.getPin());
		               dialog.dismiss();
		            }
		         });
		         pin.show();
		         return true;
		      }
		   });

	   final Handler ledger2FAHandler = new Handler(new Handler.Callback() {
		      @Override
		      public boolean handleMessage(Message message) {
		    	 Bundle messageData = message.getData();
		         LedgerPin2FADialog pin = new LedgerPin2FADialog(LedgerSignTransactionActivity.this, messageData.getString(MESSAGE_ADDRESS), messageData.getByteArray(MESSAGE_KEYCARD_INDEXES));
		         pin.setTitle(messageData.getInt(MESSAGE_TITLE_ID));
		         pin.setOnPinValid(new LedgerPin2FADialog.OnPinEntered(){
		            @Override
		            public void pinEntered(LedgerPin2FADialog dialog, Pin pin) {
		               ledgerPinResponse.add(pin.getPin());
		               dialog.dismiss();
		            }
		         });
		         pin.show();
		         return true;
		      }
		   });
	   
	
	@Override
	public String onPinRequest() {
	      // open the pin-entry dialog on the UI-Thread
	      ledgerPinHandler.sendMessage(formatMessage(R.string.ledger_enter_pin));

	      try {
	         // this call blocks until the users has entered the pin and it got added to the Queue
	         String pin = ledgerPinResponse.take();
	         return pin;
	      } catch (InterruptedException e) {
	         return "";
	      }
	}
	
	   final Handler disconnectHandler = new Handler(new Handler.Callback()  {
		      @Override
		      public boolean handleMessage(Message message) {
				  ((TextView)findViewById(R.id.tvPluginLedger)).setText(getString(R.string.ledger_powercycle));
				  findViewById(R.id.tvPluginLedger).setVisibility(View.VISIBLE);				  
				  updateUi();
		         return true;
		      }
		   });
	
	   final Handler connectHandler = new Handler(new Handler.Callback()  {
		      @Override
		      public boolean handleMessage(Message message) {
				  ((TextView)findViewById(R.id.tvPluginLedger)).setText(getString(R.string.ledger_please_wait));
				  _showTx = false;
				  updateUi();
		         return true;
		      }
		   });

	   
	@Override
	public String onUserConfirmationRequest(BTChipOutput output) {		
		if (!output.isConfirmationNeeded()) {
			return "";
		}
		else
		if (output.getUserConfirmation().equals(UserConfirmation.KEYBOARD) ||
			output.getUserConfirmation().equals(UserConfirmation.KEYCARD_NFC)) {
			// Prefer the second factor confirmation to the keycard if initiated from another interface in a multi interface product
			return onUserConfirmationRequestKeyboard();
		}
		else
		if (output.getUserConfirmation().equals(UserConfirmation.KEYCARD) ||
			output.getUserConfirmation().equals(UserConfirmation.KEYCARD_SCREEN) ||
			output.getUserConfirmation().equals(UserConfirmation.KEYCARD_DEPRECATED)) {
			return onUserConfirmationRequest2FA(output);
		}
		return "";
	}
	
	private String onUserConfirmationRequest2FA(BTChipOutput outputParam) {
		BTChipOutputKeycard output = (BTChipOutputKeycard)outputParam;
        ArrayList<String> toAddresses = new ArrayList<String>(1);
        for (TransactionOutput o : _unsigned.getOutputs()){
           Address toAddress;
           toAddress = o.script.getAddress(_mbwManager.getNetwork());
           Optional<Integer[]> addressId = ((Bip44Account) _account).getAddressId(toAddress);

           if (! (addressId.isPresent() && addressId.get()[0]==1) ){
              // this output goes to a foreign address (addressId[0]==1 means its internal change)
              toAddresses.add(toAddress.toString());
           }
        }
        ledger2FAHandler.sendMessage(formatMessage(R.string.ledger_enter_2fa_pin, toAddresses.get(0), output.getKeycardIndexes()));
	    try {
	    	// this call blocks until the users has entered the pin and it got added to the Queue
	    	String pin = ledgerPinResponse.take();
	    	// 2fa expects a binary PIN, hence ugly hack
	    	try {
	    		byte[] binaryPin = new byte[pin.length()];
	    		for (int i=0; i<pin.length(); i++) {
	    			binaryPin[i] = (byte)Integer.parseInt(pin.substring(i, i + 1), 16);
	    		}
	    		pin = new String(binaryPin, "ISO-8859-1");
	    	}
	    	catch(UnsupportedEncodingException e) {	    		
	    	}
	    	return pin;
	    } catch (InterruptedException e) {
	    	return "";
	    }						
	}
	
	private String onUserConfirmationRequestKeyboard() {
		  _showTx = true;
		  disconnectHandler.sendEmptyMessage(0);
		  while (ledgerManager.isPluggedIn()) {
			  try {
				  Thread.sleep(PAUSE_DELAY);
			  }
			  catch(InterruptedException e) {				  
			  }		  
		  }
		  connectHandler.sendEmptyMessage(0);
		  while (!ledgerManager.isPluggedIn()) {
			  try {
				  Thread.sleep(PAUSE_DELAY);
			  }
			  catch(InterruptedException e) {				  
			  }
		  }
		  		  
	      // open the pin-entry dialog on the UI-Thread
		  ledgerPinHandler.sendMessage(formatMessage(R.string.ledger_enter_transaction_pin));

	      try {
	         // this call blocks until the users has entered the pin and it got added to the Queue
	         String pin = ledgerPinResponse.take();
	         return pin;
	      } catch (InterruptedException e) {
	         return "";
	      }				
	}
	   
}
