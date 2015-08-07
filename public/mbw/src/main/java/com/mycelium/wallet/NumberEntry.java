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

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.math.BigDecimal;

public class NumberEntry {

   private static final int DEL = -1;
   private static final int DOT = -2;
   private static final int MAX_DIGITS_BEFORE_DOT = 9;

   public interface NumberEntryListener {
      public void onEntryChanged(String entry, boolean wasSet);
   }

   private NumberEntryListener _listener;
   private LinearLayout _llNumberEntry;
   private String _entry;
   private int _maxDecimals;

   public NumberEntry(int maxDecimals, NumberEntryListener listener, Activity parent) {
      this(maxDecimals, listener, parent, "");
   }

   public NumberEntry(int maxDecimals, NumberEntryListener listener, Activity parent, String text) {
      if (text.length() != 0) {
         try {
            text = new BigDecimal(text).toPlainString();
         } catch (Exception e) {
            text = "";
         }
      }
      _entry = text;
      _maxDecimals = maxDecimals;
      _listener = listener;
      _llNumberEntry = (LinearLayout) parent.findViewById(R.id.llNumberEntry);
      setClickListener((Button) _llNumberEntry.findViewById(R.id.btOne), 1);
      setClickListener((Button) _llNumberEntry.findViewById(R.id.btTwo), 2);
      setClickListener((Button) _llNumberEntry.findViewById(R.id.btThree), 3);
      setClickListener((Button) _llNumberEntry.findViewById(R.id.btFour), 4);
      setClickListener((Button) _llNumberEntry.findViewById(R.id.btFive), 5);
      setClickListener((Button) _llNumberEntry.findViewById(R.id.btSix), 6);
      setClickListener((Button) _llNumberEntry.findViewById(R.id.btSeven), 7);
      setClickListener((Button) _llNumberEntry.findViewById(R.id.btEight), 8);
      setClickListener((Button) _llNumberEntry.findViewById(R.id.btNine), 9);
      if (_maxDecimals > 0) {
         setClickListener((Button) _llNumberEntry.findViewById(R.id.btDot), DOT);
      } else{
         ((Button) _llNumberEntry.findViewById(R.id.btDot)).setText("");
      }
      setClickListener((Button) _llNumberEntry.findViewById(R.id.btZero), 0);
      setClickListener((Button) _llNumberEntry.findViewById(R.id.btDel), DEL);

      _llNumberEntry.findViewById(R.id.btDel).setOnLongClickListener(new View.OnLongClickListener() {
         @Override
         public boolean onLongClick(View v) {
            _entry = "";
            _listener.onEntryChanged(_entry, false);
            return true;
         }
      });
   }

   private void clicked(int digit) {
      if (digit == DEL) {
         // Delete Digit
         if (_entry.length() == 0) {
            return;
         }
         _entry = _entry.substring(0, _entry.length() - 1);
      } else if (digit == DOT) {
         // Do we already have a dot?
         if (hasDot()) {
            return;
         }
         if (_maxDecimals == 0) {
            return;
         }
         if (_entry.length() == 0) {
            _entry = "0.";
         } else {
            _entry = _entry + '.';
         }
      } else {
         // Append Digit
         if (digit == 0 && _entry.equals("0")) {
            // Only one leading zero
            return;
         }
         if (hasDot()) {
            if (decimalsAfterDot() >= _maxDecimals) {
               // too many decimals
               return;
            }
         } else {
            if (decimalsBeforeDot() >= MAX_DIGITS_BEFORE_DOT) {
               return;
            }
         }
         _entry = _entry + (digit);
      }
      _listener.onEntryChanged(_entry, false);
   }

   private boolean hasDot() {
      return _entry.indexOf('.') != -1;
   }

   private int decimalsAfterDot() {
      int dotIndex = _entry.indexOf('.');
      if (dotIndex == -1) {
         return 0;
      }
      return _entry.length() - dotIndex - 1;
   }

   private int decimalsBeforeDot() {
      int dotIndex = _entry.indexOf('.');
      if (dotIndex == -1) {
         return _entry.length();
      }
      return dotIndex;
   }

   public String getEntry() {
      return _entry;
   }

   public void setEntry(BigDecimal number, int maxDecimals) {
      _maxDecimals = maxDecimals;
      if (number == null || number.compareTo(BigDecimal.ZERO) == 0) {
         _entry = "";
      } else {
         _entry = number.setScale(_maxDecimals, BigDecimal.ROUND_HALF_DOWN).stripTrailingZeros().toPlainString();
      }
      _listener.onEntryChanged(_entry, true);
   }

   public BigDecimal getEntryAsBigDecimal() {
      if (_entry.length() == 0) {
         return BigDecimal.ZERO;
      }
      if (_entry.equals("0.")) {
         return BigDecimal.ZERO;
      }
      try {
         return new BigDecimal(_entry);
      } catch (NumberFormatException e) {
         return BigDecimal.ZERO;
      }
   }

   private void setClickListener(Button button, final int digit) {
      button.setOnClickListener(new android.view.View.OnClickListener() {

         @Override
         public void onClick(View v) {
            clicked(digit);
         }
      });

   }

   public void setEnabled(boolean enabled) {
      _llNumberEntry.findViewById(R.id.btOne).setEnabled(enabled);
      _llNumberEntry.findViewById(R.id.btTwo).setEnabled(enabled);
      _llNumberEntry.findViewById(R.id.btThree).setEnabled(enabled);
      _llNumberEntry.findViewById(R.id.btFour).setEnabled(enabled);
      _llNumberEntry.findViewById(R.id.btFive).setEnabled(enabled);
      _llNumberEntry.findViewById(R.id.btSix).setEnabled(enabled);
      _llNumberEntry.findViewById(R.id.btSeven).setEnabled(enabled);
      _llNumberEntry.findViewById(R.id.btEight).setEnabled(enabled);
      _llNumberEntry.findViewById(R.id.btNine).setEnabled(enabled);
      _llNumberEntry.findViewById(R.id.btDot).setEnabled(enabled);
      _llNumberEntry.findViewById(R.id.btZero).setEnabled(enabled);
      _llNumberEntry.findViewById(R.id.btDel).setEnabled(enabled);
   }

}
