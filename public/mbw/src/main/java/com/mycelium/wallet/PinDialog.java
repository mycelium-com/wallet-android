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

//todo maybe convert to Fragment, if AndroidMinVersion increased
public class PinDialog extends Dialog {

   private final Button btn_back;
   private final Button btn_clear;

   public interface OnPinEntered {
      void pinEntered(PinDialog dialog, Pin pin);
   }

   private ArrayList<Button> _btn = new ArrayList<Button>(10);
   private ArrayList<TextView> _disp = new ArrayList<TextView>(6);
   protected String _enteredPin;

   protected OnPinEntered _onPinValid = null;
   private boolean _hidden;

   public void setOnPinValid(OnPinEntered _onPinValid) {
      this._onPinValid = _onPinValid;
   }


   public PinDialog(Context context, boolean hidden) {
      super(context);
      setLayout();
      _hidden = hidden;
      _disp.add( (TextView) findViewById(R.id.pin_char_1));
      _disp.add( (TextView) findViewById(R.id.pin_char_2));
      _disp.add( (TextView) findViewById(R.id.pin_char_3));
      _disp.add( (TextView) findViewById(R.id.pin_char_4));
      _disp.add( (TextView) findViewById(R.id.pin_char_5));
      _disp.add( (TextView) findViewById(R.id.pin_char_6));
      _btn.add( ((Button) findViewById(R.id.pin_button0)));
      _btn.add( ((Button) findViewById(R.id.pin_button1)));
      _btn.add( ((Button) findViewById(R.id.pin_button2)));
      _btn.add( ((Button) findViewById(R.id.pin_button3)));
      _btn.add( ((Button) findViewById(R.id.pin_button4)));
      _btn.add( ((Button) findViewById(R.id.pin_button5)));
      _btn.add( ((Button) findViewById(R.id.pin_button6)));
      _btn.add( ((Button) findViewById(R.id.pin_button7)));
      _btn.add( ((Button) findViewById(R.id.pin_button8)));
      _btn.add( ((Button) findViewById(R.id.pin_button9)));

      btn_clear = (Button) findViewById(R.id.pin_clr);
      btn_back = (Button) findViewById(R.id.pin_back);

      _enteredPin = "";

      int cnt=0;
      for (Button b : _btn) {
         final int akCnt = cnt;
         b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               addDigit(String.valueOf(akCnt));
            }
         });
         cnt++;
      }

      btn_back.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            removeLastDigit();
         }
      });

      btn_clear.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            clearDigits();
         }
      });

      this.setTitle(R.string.pin_enter_pin);
   }

   protected void setLayout() {
      setContentView(R.layout.enter_pin_dialog);
   }

   private void addDigit(String c) {
      _enteredPin = _enteredPin + c;
      updatePinDisplay();
   }

   private void updatePinDisplay(){
      int cnt = 0;
      for (TextView t : _disp) {
         t.setText(getPinDigitAsString(_enteredPin, cnt));
         cnt++;
      }
      checkPin();
   }

   private String getPinDigitAsString(String pin, int index) {
      if (pin.length() > index) {
         return _hidden ? "*" : pin.substring(index, index + 1);
      } else {
         return "";
      }
   }

   private void clearDigits() {
      _enteredPin = "";
      for (TextView t : _disp) {
         t.setText("");
      }
   }

   private void removeLastDigit(){
      if (!Strings.isNullOrEmpty(_enteredPin)){
         _enteredPin = _enteredPin.substring(0, _enteredPin.length() - 1);
      }
      updatePinDisplay();
   }

   private void enableButtons(boolean enabled) {
      for (Button b : _btn) {
         b.setEnabled(enabled);
      }
   }

   private void checkPin() {
      if (_enteredPin.length() >= 6) {
         enableButtons(false);
         delayhandler.sendMessage(delayhandler.obtainMessage());
      }
   }



   /**
    * Trick to make the last digit update before the dialog is disabled
    */
  final Handler delayhandler = new Handler() {
      public void handleMessage(Message msg) {
         if (_onPinValid != null) _onPinValid.pinEntered(PinDialog.this, getPin());
         enableButtons(true);
         clearDigits();
      }
   };

   protected Pin getPin() {
      return new Pin(_enteredPin);
   }


}

