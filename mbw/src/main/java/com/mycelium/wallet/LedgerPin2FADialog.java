package com.mycelium.wallet;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.google.common.base.Strings;
import com.mycelium.wallet.activity.util.Pin;

import java.util.ArrayList;

public class LedgerPin2FADialog extends Dialog {
   protected Button btnBack;
   protected Button btnClear;

   public interface OnPinEntered {
      void pinEntered(LedgerPin2FADialog dialog, Pin pin);
   }

   protected ArrayList<Button> buttons = new ArrayList<Button>(16);
   protected String enteredPin;

   protected OnPinEntered onPinValid = null;
   private String address;
   private byte[] keycardIndexes;
   private TextView pinDisp;
   private TextView addrInfoHighlight;
   private TextView addrInfoPrefix;
   private TextView addrInfoPostfix;

   public void setOnPinValid(OnPinEntered _onPinValid) {
      this.onPinValid = _onPinValid;
   }

   public void setDialogTitle(int titleRes) {
      this.setTitle(titleRes);
   }

   public LedgerPin2FADialog(Context context, String address, byte[] keycardIndexes) {
      super(context);
      this.address = address;
      this.keycardIndexes = keycardIndexes.clone();
      this.setCanceledOnTouchOutside(false);
      enteredPin = "";
      loadLayout();
      initPinPad();
      this.setTitle(R.string.pin_enter_pin);
   }

   protected void initPinPad() {
      pinDisp = (TextView) findViewById(R.id.pin_display);
      addrInfoPrefix = (TextView) findViewById(R.id.pin_addr_info_prefix);
      addrInfoHighlight = (TextView) findViewById(R.id.pin_addr_info_highlight);
      addrInfoPostfix = (TextView) findViewById(R.id.pin_addr_info_postfix);
      buttons.add(((Button) findViewById(R.id.pin_hexbutton0)));
      buttons.add(((Button) findViewById(R.id.pin_hexbutton1)));
      buttons.add(((Button) findViewById(R.id.pin_hexbutton2)));
      buttons.add(((Button) findViewById(R.id.pin_hexbutton3)));
      buttons.add(((Button) findViewById(R.id.pin_hexbutton4)));
      buttons.add(((Button) findViewById(R.id.pin_hexbutton5)));
      buttons.add(((Button) findViewById(R.id.pin_hexbutton6)));
      buttons.add(((Button) findViewById(R.id.pin_hexbutton7)));
      buttons.add(((Button) findViewById(R.id.pin_hexbutton8)));
      buttons.add(((Button) findViewById(R.id.pin_hexbutton9)));
      buttons.add(((Button) findViewById(R.id.pin_hexbuttonA)));
      buttons.add(((Button) findViewById(R.id.pin_hexbuttonB)));
      buttons.add(((Button) findViewById(R.id.pin_hexbuttonC)));
      buttons.add(((Button) findViewById(R.id.pin_hexbuttonD)));
      buttons.add(((Button) findViewById(R.id.pin_hexbuttonE)));
      buttons.add(((Button) findViewById(R.id.pin_hexbuttonF)));

      btnClear = (Button) findViewById(R.id.pin_clrhex);
      btnBack = (Button) findViewById(R.id.pin_backhex);
      btnBack.setText("OK");
      int cnt = 0;
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

   protected void updatePinDisplay() {
      if (enteredPin.length() < keycardIndexes.length) {
         int currentOffset = keycardIndexes[enteredPin.length()];
         int showSurroundingChars = 8;
         int start = Math.max(currentOffset - showSurroundingChars, 0);
         int end = Math.min(currentOffset + showSurroundingChars, address.length());

         String prefix = address.substring(start, currentOffset);
         String postfix = address.substring(currentOffset + 1, end);

         if (currentOffset - showSurroundingChars > 0){
            prefix = "..." + prefix;
         }

         if (currentOffset + showSurroundingChars < address.length()){
            postfix += "...";
         }

         addrInfoPrefix.setText(prefix);
         addrInfoHighlight.setText(address.substring(currentOffset, currentOffset+1));
         addrInfoPostfix.setText(postfix);
      }
      pinDisp.setText(
            Strings.repeat(PinDialog.PLACEHOLDER_TYPED, enteredPin.length()) +
                  Strings.repeat(PinDialog.PLACEHOLDER_NOT_TYPED, keycardIndexes.length - enteredPin.length())
      );
      checkPin();
   }

   protected void clearDigits() {
      enteredPin = "";
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
         if (onPinValid != null) {
            onPinValid.pinEntered(LedgerPin2FADialog.this, getPin());
         }
         enableButtons(true);
         clearDigits();
      }
   };

   protected Pin getPin() {
      return new Pin(enteredPin);
   }
}
