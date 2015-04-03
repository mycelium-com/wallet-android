package com.mycelium.wallet;

import java.util.ArrayList;

import com.mycelium.wallet.activity.util.Pin;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.google.common.base.Strings;

public class LedgerPin2FADialog extends Dialog {
	
	   protected Button btnBack;
	   protected Button btnClear;

	   public interface OnPinEntered {
	      void pinEntered(LedgerPin2FADialog dialog, Pin pin);
	   }

	   protected ArrayList<Button> buttons = new ArrayList<Button>(16);
	   protected String enteredPin;

	   protected OnPinEntered onPinValid = null;
	   private boolean hidden;
	   private String address;
	   private byte[] keycardIndexes;
	   private TextView pinDisp;
	   private TextView addrInfo;

	   public void setOnPinValid(OnPinEntered _onPinValid) {
	      this.onPinValid = _onPinValid;
	   }
	   
	   public void setDialogTitle(int titleRes) {
		   this.setTitle(titleRes);
	   }
	   
	   public LedgerPin2FADialog(Context context, String address, byte[] keycardIndexes, boolean hidden) {
	      super(context);
	      this.address = address;
	      this.keycardIndexes = keycardIndexes;	      
	      this.hidden = hidden;
	      enteredPin = "";
	      loadLayout();
	      initPinPad();	      
	      this.setTitle(R.string.pin_enter_pin);
	   }

	   protected void initPinPad() {
		  pinDisp = (TextView) findViewById(R.id.pin_display);
		  addrInfo = (TextView) findViewById(R.id.pin_addr_info);
	      buttons.add( ((Button) findViewById(R.id.pin_hexbutton0)));
	      buttons.add( ((Button) findViewById(R.id.pin_hexbutton1)));
	      buttons.add( ((Button) findViewById(R.id.pin_hexbutton2)));
	      buttons.add( ((Button) findViewById(R.id.pin_hexbutton3)));
	      buttons.add( ((Button) findViewById(R.id.pin_hexbutton4)));
	      buttons.add( ((Button) findViewById(R.id.pin_hexbutton5)));
	      buttons.add( ((Button) findViewById(R.id.pin_hexbutton6)));
	      buttons.add( ((Button) findViewById(R.id.pin_hexbutton7)));
	      buttons.add( ((Button) findViewById(R.id.pin_hexbutton8)));
	      buttons.add( ((Button) findViewById(R.id.pin_hexbutton9)));
	      buttons.add( ((Button) findViewById(R.id.pin_hexbuttonA)));
	      buttons.add( ((Button) findViewById(R.id.pin_hexbuttonB)));
	      buttons.add( ((Button) findViewById(R.id.pin_hexbuttonC)));
	      buttons.add( ((Button) findViewById(R.id.pin_hexbuttonD)));
	      buttons.add( ((Button) findViewById(R.id.pin_hexbuttonE)));
	      buttons.add( ((Button) findViewById(R.id.pin_hexbuttonF)));

	      btnClear = (Button) findViewById(R.id.pin_clrhex);
	      btnBack = (Button) findViewById(R.id.pin_backhex);
	      btnBack.setText("OK");
	      int cnt=0;
	      for (Button b : buttons) {
	         final int akCnt = cnt;
	         b.setOnClickListener(new View.OnClickListener() {
	            @Override
	            public void onClick(View v) {
	               addDigit(Integer.toHexString(akCnt));
	            }
	         });
	         cnt++;
	      }

	      btnBack.setOnClickListener(new View.OnClickListener() {
	         @Override
	         public void onClick(View v) {
	        	 acceptPin();
	         }
	      });

	      btnClear.setOnClickListener(new View.OnClickListener() {
	         @Override
	         public void onClick(View v) {
	            clearDigits();
	            updatePinDisplay();
	         }
	      });
	      updatePinDisplay();
	   }

	   protected void loadLayout() {
	      setContentView(R.layout.enter_ledger_pin_dialoghex);
	   }

	   protected void addDigit(String c) {
	      enteredPin = enteredPin + c;
	      updatePinDisplay();
	   }

	   protected void updatePinDisplay(){
		   if (enteredPin.length() < keycardIndexes.length) {
			   int currentOffset = keycardIndexes[enteredPin.length()];
			   String addressToMatch = "";
			   for (int i=0; i<address.length(); i++) {
				   if (i != currentOffset) {
					   addressToMatch += address.charAt(i);
				   }
				   else {
					   addressToMatch += "<b>*" + address.charAt(i) + "*</b>";
				   }
			   }
			   addrInfo.setText(Html.fromHtml(addressToMatch));
		   }
		   pinDisp.setText(Strings.repeat("* ", enteredPin.length()));
		   checkPin();
	   }

	   protected String getPinDigitAsString(String pin, int index) {
	      if (pin.length() > index) {
	         return hidden ? "*" : pin.substring(index, index + 1);
	      } else {
	         return " ";
	      }
	   }

	   protected void clearDigits() {
	      enteredPin = "";
	   }

	   protected void removeLastDigit(){
	      if (!Strings.isNullOrEmpty(enteredPin)){
	         enteredPin = enteredPin.substring(0, enteredPin.length() - 1);
	      }
	      updatePinDisplay();
	   }

	   protected void enableButtons(boolean enabled) {
	      for (Button b : buttons) {
	         b.setEnabled(enabled);
	      }
	   }

	   protected void checkPin() {
		      if (enteredPin.length() == keycardIndexes.length) {
		         acceptPin();
		      }
	   }

	   
	   protected void acceptPin() {
	      enableButtons(false);
	      delayHandler.sendMessage(delayHandler.obtainMessage());
	   }


	   /**
	    * Trick to make the last digit update before the dialog is disabled
	    */
	  final Handler delayHandler = new Handler() {
	      public void handleMessage(Message msg) {
	         if (onPinValid != null) onPinValid.pinEntered(LedgerPin2FADialog.this, getPin());
	         enableButtons(true);
	         clearDigits();
	      }
	   };

	   protected Pin getPin() {
	      return new Pin(enteredPin);
	   }
	
}
