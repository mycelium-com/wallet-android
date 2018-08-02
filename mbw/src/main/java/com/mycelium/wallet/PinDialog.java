/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatDialog;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import com.google.common.base.Strings;
import com.mycelium.wallet.activity.util.Pin;

import java.util.ArrayList;
import java.util.Collections;

public class PinDialog extends AppCompatDialog {
   public static final String PLACEHOLDER_TYPED = "\u25CF"; // Unicode Character 'BLACK CIRCLE' (which is a white circle in our dark theme)
   public static final String PLACEHOLDER_NOT_TYPED = "\u25CB"; // Unicode Character 'WHITE CIRCLE' (which is a black circle)
   public static final String PLACEHOLDER_SMALL = "\u2022"; // Unicode Character  'BULLET'
   protected Button btnBack;
   protected Button btnClear;

   public interface OnPinEntered {
      void pinEntered(PinDialog dialog, Pin pin);
   }

   protected ArrayList<Button> buttons = new ArrayList<Button>(10);
   protected ArrayList<TextView> disps = new ArrayList<TextView>(6);
   protected String enteredPin;

   protected OnPinEntered onPinValid = null;
   private boolean hidden;
   protected boolean pinPadIsRandomized;

   public void setOnPinValid(OnPinEntered _onPinValid) {
      this.onPinValid = _onPinValid;
   }

   public PinDialog(Context context, boolean hidden, boolean cancelable) {
      super(context);
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
      pinPadIsRandomized = MbwManager.getInstance(context).isPinPadRandomized();
      this.hidden = hidden;
      setCancelable(cancelable);
      setCanceledOnTouchOutside(false);
      loadLayout();
      initPinPad();
      enteredPin = "";
      clearDigits();
      updatePinDisplay();

      this.setTitle(R.string.pin_enter_pin);
   }

   protected void initPinPad() {
      disps.add((TextView) findViewById(R.id.pin_char_1));
      disps.add((TextView) findViewById(R.id.pin_char_2));
      disps.add((TextView) findViewById(R.id.pin_char_3));
      disps.add((TextView) findViewById(R.id.pin_char_4));
      disps.add((TextView) findViewById(R.id.pin_char_5));
      disps.add((TextView) findViewById(R.id.pin_char_6));
      buttons.add( ((Button) findViewById(R.id.pin_button0)));
      buttons.add( ((Button) findViewById(R.id.pin_button1)));
      buttons.add( ((Button) findViewById(R.id.pin_button2)));
      buttons.add( ((Button) findViewById(R.id.pin_button3)));
      buttons.add( ((Button) findViewById(R.id.pin_button4)));
      buttons.add( ((Button) findViewById(R.id.pin_button5)));
      buttons.add( ((Button) findViewById(R.id.pin_button6)));
      buttons.add( ((Button) findViewById(R.id.pin_button7)));
      buttons.add( ((Button) findViewById(R.id.pin_button8)));
      buttons.add( ((Button) findViewById(R.id.pin_button9)));

      ArrayList<Integer> numbers = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
         numbers.add(i);
      }
      if (pinPadIsRandomized) {
         Collections.shuffle(numbers);
      }
      for (int i = 0; i < 10; i++) {
         buttons.get(i).setText(numbers.get(i).toString());
      }

      btnClear = (Button) findViewById(R.id.pin_clr);
      btnBack = (Button) findViewById(R.id.pin_back);

      for (Button b : buttons) {
         final int num = Integer.parseInt(b.getText().toString());
         b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               addDigit(String.valueOf(num));
            }
         });
      }

      btnBack.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            removeLastDigit();
         }
      });

      btnClear.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            clearDigits();
            updatePinDisplay();
         }
      });
   }

   protected void loadLayout() {
      setContentView(R.layout.enter_pin_dialog);
   }

   protected void addDigit(String c) {
      enteredPin = enteredPin + c;
      updatePinDisplay();
   }

   protected void updatePinDisplay(){
      int cnt = 0;
      for (TextView t : disps) {
         t.setText(getPinDigitAsString(enteredPin, cnt));
         cnt++;
      }
      checkPin();
   }

   protected String getPinDigitAsString(String pin, int index) {
      if (pin.length() > index) {
         return hidden ? PLACEHOLDER_TYPED : pin.substring(index, index + 1);
      } else {
         return hidden ? PLACEHOLDER_NOT_TYPED : PLACEHOLDER_SMALL;
      }
   }

   protected void clearDigits() {
      enteredPin = "";
      for (TextView t : disps) {
         t.setText(hidden ? PLACEHOLDER_NOT_TYPED : PLACEHOLDER_SMALL);
      }
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
      if (enteredPin.length() >= 6) {
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
         if (onPinValid != null) onPinValid.pinEntered(PinDialog.this, getPin());
         enableButtons(true);
         clearDigits();
      }
   };

   protected Pin getPin() {
      return new Pin(enteredPin);
   }
}

