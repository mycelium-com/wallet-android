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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import com.google.common.base.Optional;
import com.mycelium.wallet.activity.util.Pin;


public class ClearPinDialog extends PinDialog {
   public ClearPinDialog(final Context context, boolean hidden) {
      super(context, hidden, true);
      final MbwManager mbwManager = MbwManager.getInstance(context);

      Button btnForgotPin = (Button) findViewById(R.id.btn_forgot_pin);

      if (mbwManager.getPin().isResettable()){
         Optional<Integer> resetPinRemainingBlocksCount = mbwManager.getResetPinRemainingBlocksCount();
         if (resetPinRemainingBlocksCount.or(1) == 0){
            // reset procedure was started and is already old enough -> provide option to reset PIN
            btnForgotPin.setText("Reset PIN now");
            btnForgotPin.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                  mbwManager.savePin(Pin.CLEAR_PIN);
                  if (onPinValid != null) onPinValid.pinEntered(ClearPinDialog.this, Pin.CLEAR_PIN );
               }
            });
         }else if (resetPinRemainingBlocksCount.isPresent()){
            // reset procedure was started, but the target blockheight isn't reached
            btnForgotPin.setText(String.format(
                  context.getString(R.string.pin_forgotten_reset_wait_button_text),
                  Utils.formatBlockcountAsApproxDuration(this.getContext(), resetPinRemainingBlocksCount.get()))
            );

            btnForgotPin.setEnabled(false);
         }else{
            // no reset procedure was started
            btnForgotPin.setOnClickListener(startResetListener(context, mbwManager));
         }

      }else{
         // The current PIN is not marked as resettable - sorry, you are on your own
         btnForgotPin.setVisibility(View.GONE);
      }
   }

   private View.OnClickListener startResetListener(final Context context, final MbwManager mbwManager) {
      return new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            new AlertDialog.Builder(ClearPinDialog.this.getContext())
                  .setPositiveButton(context.getString(R.string.yes), new OnClickListener() {
                     @Override
                     public void onClick(DialogInterface dialog, int which) {
                        mbwManager.startResetPinProcedure();
                        ClearPinDialog.this.dismiss();
                     }
                  })
                  .setNegativeButton(context.getString(R.string.no), new OnClickListener() {
                     @Override
                     public void onClick(DialogInterface dialog, int which) {
                        ClearPinDialog.this.dismiss();
                     }
                  })
                  .setMessage(context.getString(
                              R.string.pin_forgotten_reset_pin_dialog_content,
                              Utils.formatBlockcountAsApproxDuration(
                                    ClearPinDialog.this.getContext(),
                                    Constants.MIN_PIN_BLOCKHEIGHT_AGE_RESET_PIN)
                        )
                  )
                  .setTitle(context.getString(R.string.pin_forgotten_reset_pin_dialog_title))
                  .show();
         }
      };
   }

   @Override
   protected void loadLayout() {
      setContentView(R.layout.enter_clear_pin_dialog);
   }
}
