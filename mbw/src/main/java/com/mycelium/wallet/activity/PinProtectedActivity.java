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

package com.mycelium.wallet.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.PinDialog;
import com.mycelium.wallet.R;

public class PinProtectedActivity extends Activity{

   private static final String START_ACTIVITY = "startActivity";
   private PinDialog pinDialog;

   public static Intent createIntent(Context context, Intent startActivityOnValidPin) {
      Intent intent = new Intent(context, PinProtectedActivity.class);
      intent.putExtra(START_ACTIVITY, startActivityOnValidPin);
      intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
      return intent;
   }

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      requestWindowFeature(Window.FEATURE_NO_TITLE);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.startup_activity);

      final MbwManager _mbwManager = MbwManager.getInstance(this);
      final Intent startActivity = getIntent().getParcelableExtra(START_ACTIVITY);

      final Runnable startNextActivity = new Runnable() {
         @Override
         public void run() {
            _mbwManager.setStartUpPinUnlocked(true);
            PinProtectedActivity.this.startActivity(startActivity);
            PinProtectedActivity.this.finish();
         }
      };
      if (_mbwManager.isUnlockPinRequired()) {
         showPinDialog(_mbwManager, startNextActivity);
      } else {
         // no Startup Pin required... just patch it through
         PinProtectedActivity.this.startActivity(startActivity);
         finish();
      }

      // set a TapHandler, if you click the background, show the pin dialog
      getWindow().getDecorView().findViewById(android.R.id.content).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            showPinDialog(_mbwManager, startNextActivity);
         }
      });
   }

   private void showPinDialog(MbwManager _mbwManager, Runnable startNextActivity) {
      pinDialog = _mbwManager.runPinProtectedFunction(this, startNextActivity, false);
   }

   @Override
   protected void onStop() {
      super.onStop();
      if (pinDialog != null){
         pinDialog.dismiss();
      }
   }
}
