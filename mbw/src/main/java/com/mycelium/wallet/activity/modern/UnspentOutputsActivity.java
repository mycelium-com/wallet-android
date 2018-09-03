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

package com.mycelium.wallet.activity.modern;

import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.util.AddressLabel;
import com.mycelium.wapi.model.TransactionOutputSummary;
import com.mycelium.wapi.wallet.WalletAccount;

import java.util.List;
import java.util.UUID;

public class UnspentOutputsActivity extends Activity {
   private static final LinearLayout.LayoutParams WCWC = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
   private static final LinearLayout.LayoutParams FPWC = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);

   private MbwManager _mbwManager;
   private UUID _accountid;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.unspent_outputs_activity);

      _mbwManager = MbwManager.getInstance(this.getApplication());
      _accountid = (UUID) getIntent().getSerializableExtra("account");
      updateUi();
   }

   private void updateUi() {
      LinearLayout outputView = findViewById(R.id.listUnspentOutputs);
      WalletAccount account = _mbwManager.getWalletManager(false).getAccount(_accountid);
      List<TransactionOutputSummary> outputs = account.getUnspentTransactionOutputSummary();

      if (outputs.isEmpty()) {
         findViewById(R.id.tvNoOutputs).setVisibility(View.VISIBLE);
      } else {
         findViewById(R.id.tvNoOutputs).setVisibility(View.GONE);
      }

      for (TransactionOutputSummary item : outputs) {
         outputView.addView(getItemView(item));
      }
      if (!(outputs.size()<=5)) {
         TextView noOutputs = findViewById(R.id.tvOutputsTitle);
         noOutputs.append(" (" + String.valueOf(outputs.size()) + ")");
      }
   }

   private View getItemView(TransactionOutputSummary item) {
      // Create vertical linear layout for address
      LinearLayout ll = new LinearLayout(this);
      ll.setOrientation(LinearLayout.VERTICAL);
      ll.setLayoutParams(WCWC);
      ll.setPadding(10, 10, 10, 10);

      // Add BTC value
      ll.addView(getValue(item.value));

      AddressLabel addressLabel = new AddressLabel(this);
      addressLabel.setAddress(item.address);
      ll.addView(addressLabel);

      return ll;
   }

   private View getValue(long value) {
      TextView tv = new TextView(this);
      tv.setLayoutParams(FPWC);
      tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
      tv.setText(_mbwManager.getBtcValueString(value));
      tv.setTextColor(getResources().getColor(R.color.white));
      return tv;
   }
}
