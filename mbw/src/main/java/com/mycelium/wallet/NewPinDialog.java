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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.mycelium.wallet.activity.util.Pin;

public class NewPinDialog extends PinDialog {
   private final CheckBox cbResettablePin;

   public NewPinDialog(final Context context, boolean hidden) {
      super(context, hidden, true);
      this.setTitle(R.string.pin_enter_new_pin);

      MbwManager mbwManager = MbwManager.getInstance(context);
      cbResettablePin = (CheckBox) findViewById(R.id.cb_resettable_pin);

      cbResettablePin.setChecked(mbwManager.getPin().isSet() );

      cbResettablePin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
         @Override
         public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            updateResetInfo(context);
         }
      });

      updateResetInfo(context);
   }

   private void updateResetInfo(Context context) {
      TextView txtInfo = (TextView) findViewById(R.id.tv_resettable_pin_info);
      if (cbResettablePin.isChecked()) {
         txtInfo.setText(context.getString(
               R.string.pin_resettable_pin_info,
               Utils.formatBlockcountAsApproxDuration(this.getContext(), Constants.MIN_PIN_BLOCKHEIGHT_AGE_RESET_PIN)
         ));
      } else {
         txtInfo.setText(context.getString(R.string.pin_unresettable_pin_info));
      }
   }

   @Override
   protected void loadLayout() {
      setContentView(R.layout.enter_new_pin_dialog);
   }

   @Override
   protected Pin getPin() {
      return new Pin(enteredPin, isResettable());
   }

   public boolean isResettable(){
      return cbResettablePin.isChecked();
   }
}
