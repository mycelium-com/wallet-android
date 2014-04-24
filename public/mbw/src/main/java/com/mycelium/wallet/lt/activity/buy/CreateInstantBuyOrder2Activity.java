/*
 * Copyright 2013 Megion Research and Development GmbH
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

package com.mycelium.wallet.lt.activity.buy;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.lt.activity.SendRequestActivity;
import com.mycelium.wallet.lt.api.CreateInstantBuyOrder;

public class CreateInstantBuyOrder2Activity extends Activity {

   public static void callMe(Activity currentActivity, CreateInstantBuyOrder request) {
      Intent intent = new Intent(currentActivity, CreateInstantBuyOrder2Activity.class);
      intent.putExtra("request", request);
      currentActivity.startActivity(intent);
   }

   private CreateInstantBuyOrder _request;
   protected MbwManager _mbwManager;

   /**
    * Called when the activity is first created.
    */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.lt_create_instant_buy_order_2_activity);

      _mbwManager = MbwManager.getInstance(getApplication());

      // Get intent parameters
      _request = (CreateInstantBuyOrder) getIntent().getSerializableExtra("request");

      // Set label if applicable
      TextView addressLabel = (TextView) findViewById(R.id.tvAddressLabel);
      String label = _mbwManager.getAddressBookManager().getNameByAddress(_request.getAddress().toString());
      if (label == null || label.length() == 0) {
         // Hide label
         addressLabel.setVisibility(View.GONE);
      } else {
         // Show label
         addressLabel.setText(label);
         addressLabel.setVisibility(View.VISIBLE);
      }

      // Set Address
      ((TextView) findViewById(R.id.tvAddress)).setText(_request.getAddress().toMultiLineString());

      // Show / hide warning
      Record record = _mbwManager.getRecordManager().getRecord(_request.getAddress());
      TextView tvWarning = (TextView) findViewById(R.id.tvWarning);
      if (record != null && record.hasPrivateKey()) {
         // We send to an address where we have the private key
         findViewById(R.id.tvWarning).setVisibility(View.GONE);
      } else {
         // Show a warning as we are sending to an address where we don't have
         // the private key
         tvWarning.setVisibility(View.VISIBLE);
         tvWarning.setText(R.string.read_only_warning);
         tvWarning.setTextColor(getResources().getColor(R.color.red));
      }

      ((Button) findViewById(R.id.btStartTrading)).setOnClickListener(startTradingClickListener);
   }

   OnClickListener startTradingClickListener = new OnClickListener() {

      @Override
      public void onClick(View v) {
         SendRequestActivity.callMe(CreateInstantBuyOrder2Activity.this, _request,
               getString(R.string.lt_place_instant_buy_order_title));
         finish();
      }
   };

   @Override
   protected void onResume() {
      super.onResume();
   }

   @Override
   protected void onPause() {
      super.onPause();
   }

}
