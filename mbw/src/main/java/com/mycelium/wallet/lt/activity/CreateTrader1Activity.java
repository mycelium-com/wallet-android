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

package com.mycelium.wallet.lt.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.mycelium.wallet.R;

/**
 * CreateTrader{1|2|3}Activity are a sort of Trader Account Creation Wizard.
 * You start at 1, go to 2, finish at 3.
 */
public class CreateTrader1Activity extends Activity {
   public static void callMe(Activity currentActivity, int requestCode) {
      Intent intent = new Intent(currentActivity, CreateTrader1Activity.class);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.lt_create_trader_1_activity);

      TextView _tvDescription = findViewById(R.id.tvDescription);
      Button _btAccept = findViewById(R.id.btAccept);
      Button _btDecline = findViewById(R.id.btDecline);

      _tvDescription.setText(
            getString(R.string.lt_tos_1)
                  + "\n\n"
                  + getString(R.string.lt_tos_2) +
                  "\n\n"
                  + getString(R.string.lt_tos_3) +
                  "\n\n"
                  + getString(R.string.lt_tos_4)
      );
      _btAccept.setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            CreateTrader2Activity.callMe(CreateTrader1Activity.this);
            finish();
         }
      });

      _btDecline.setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            finish();
         }
      });
   }
}
