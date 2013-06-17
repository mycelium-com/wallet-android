/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 *  Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 *  This license governs use of the accompanying software. If you use the software, you accept this license.
 *  If you do not accept the license, do not use the software.
 *
 *  1. Definitions
 *  The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 *  "You" means the licensee of the software.
 *  "Your company" means the company you worked for when you downloaded the software.
 *  "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 *  of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 *  software, and specifically excludes the right to distribute the software outside of your company.
 *  "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 *  under this license.
 *
 *  2. Grant of Rights
 *  (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free copyright license to reproduce the software for reference use.
 *  (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free patent license under licensed patents for reference use.
 *
 *  3. Limitations
 *  (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 *  (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 *  (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 *  (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 *  guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 *  change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 *  fitness for a particular purpose and non-infringement.
 *
 */

package com.mrd.mbw.activity.receive;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mrd.bitlib.util.CoinUtil;
import com.mrd.mbw.MbwManager;
import com.mrd.mbw.R;
import com.mrd.mbw.Record;
import com.mrd.mbw.Utils;

public class ReceiveCoinsActivity extends Activity {

   public static final int SCANNER_RESULT_CODE = 0;

   private MbwManager _mbwManager;
   private String _uri;

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.receive_coins_activity);

      _mbwManager = MbwManager.getInstance(getApplication());
      Record record = _mbwManager.getRecordManager().getSelectedRecord();

      // Get intent parameters
      long amount = getIntent().getLongExtra("amount", 0);

      if (amount == 0) {
         ((TextView) findViewById(R.id.tvTitle)).setText(R.string.bitcoin_address_title);
         findViewById(R.id.tvAmountLabel).setVisibility(View.GONE);
         findViewById(R.id.tvAmount).setVisibility(View.GONE);
         // If there is no amount just use the bitcoin address instead of a
         // complete Bitcoin URI
         _uri = record.address.toString();
      } else {
         ((TextView) findViewById(R.id.tvTitle)).setText(R.string.payment_request);
         ((TextView) findViewById(R.id.tvAmount)).setText(_mbwManager.getBtcValueString(amount));
         _uri = "bitcoin:" + record.address.toString() + "?amount=" + CoinUtil.valueString(amount);
      }

      Bitmap bitmap = Utils.getLargeQRCodeBitmap(_uri, _mbwManager);
      ImageView iv = (ImageView) findViewById(R.id.ivQrCode);
      iv.setImageBitmap(bitmap);

      findViewById(R.id.btCopyToClipboard).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(_uri);
            Toast.makeText(ReceiveCoinsActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
         }
      });

      String[] addressStrings = Utils.stringChopper(record.address.toString(), 12);
      ((TextView) findViewById(R.id.tvAddress1)).setText(addressStrings[0]);
      ((TextView) findViewById(R.id.tvAddress2)).setText(addressStrings[1]);
      ((TextView) findViewById(R.id.tvAddress3)).setText(addressStrings[2]);

      Utils.fadeViewInOut(findViewById(R.id.tvSplash));
   }

}