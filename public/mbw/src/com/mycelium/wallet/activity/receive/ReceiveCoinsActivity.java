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

package com.mycelium.wallet.activity.receive;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Preconditions;

import com.mrd.bitlib.util.CoinUtil;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.Wallet;

public class ReceiveCoinsActivity extends Activity {

   public static final int SCANNER_RESULT_CODE = 0;

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.receive_coins_activity);

      MbwManager mbwManager = MbwManager.getInstance(getApplication());

      // Get intent parameters
      Wallet wallet = Preconditions.checkNotNull((Wallet) getIntent().getSerializableExtra("wallet"));
      long amount = getIntent().getLongExtra("amount", 0);

      final String uri;
      if (amount == 0) {
         ((TextView) findViewById(R.id.tvTitle)).setText(R.string.bitcoin_address_title);
         findViewById(R.id.tvAmountLabel).setVisibility(View.GONE);
         findViewById(R.id.tvAmount).setVisibility(View.GONE);
         // If there is no amount just use the bitcoin address instead of a
         // complete Bitcoin URI
         uri = wallet.getReceivingAddress().toString();
      } else {
         ((TextView) findViewById(R.id.tvTitle)).setText(R.string.payment_request);
         ((TextView) findViewById(R.id.tvAmount)).setText(mbwManager.getBtcValueString(amount));
         uri = "bitcoin:" + wallet.getReceivingAddress().toString() + "?amount=" + CoinUtil.valueString(amount);
      }

      // QR code
      Bitmap bitmap = Utils.getLargeQRCodeBitmap(uri, mbwManager);
      ImageView iv = (ImageView) findViewById(R.id.ivQrCode);
      iv.setImageBitmap(bitmap);

      // Make QR code fade along with the entire view
      findViewById(R.id.llRoot).setAnimation(AnimationUtils.loadAnimation(this, R.anim.slow_pulse));

      // Show warning if the record has no private key
      if (wallet.hasPrivateKeyForReceivingAddress()) {
         findViewById(R.id.tvWarning).setVisibility(View.GONE);
      } else {
         findViewById(R.id.tvWarning).setVisibility(View.VISIBLE);
      }

      findViewById(R.id.btCopyToClipboard).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(uri);
            Toast.makeText(ReceiveCoinsActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
         }
      });

      String[] addressStrings = Utils.stringChopper(wallet.getReceivingAddress().toString(), 12);
      ((TextView) findViewById(R.id.tvAddress1)).setText(addressStrings[0]);
      ((TextView) findViewById(R.id.tvAddress2)).setText(addressStrings[1]);
      ((TextView) findViewById(R.id.tvAddress3)).setText(addressStrings[2]);

      Utils.fadeViewInOut(findViewById(R.id.tvSplash));
   }

}