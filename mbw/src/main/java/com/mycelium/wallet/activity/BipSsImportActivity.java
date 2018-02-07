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

import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.TextView;
import com.mrd.bitlib.crypto.BipSss;
import com.mycelium.wallet.R;
import com.mycelium.wallet.StringHandleConfig;

public class BipSsImportActivity extends Activity {
   public static final String RESULT_SECRET = "secret";
   private static final int REQUEST_SHARE_CODE = 1;

   public static void callMe(Activity currentActivity, BipSss.Share share, int requestCode) {
      Intent intent = new Intent(currentActivity, BipSsImportActivity.class)
              .putExtra("share", share);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   private List<BipSss.Share> shares = new ArrayList<BipSss.Share>();

   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_bip_ss_import);

      BipSss.Share share = (BipSss.Share) getIntent().getSerializableExtra("share");
      shares.add(share);

      findViewById(R.id.btScan).setOnClickListener(scanClickListener);
      updateUI();
   }

   private void updateUI() {
      BipSss.Share last = shares.get(shares.size() -1);
      String status = getString(R.string.sss_share_number_scanned, last.shareNumber);
      try {
         String secret = BipSss.combine(shares);

         // Success, send the result back immediately
         Intent result = new Intent();
         result.putExtra(RESULT_SECRET, secret);
         setResult(RESULT_OK, result);
         finish();
         return;

      } catch (BipSss.IncompatibleSharesException e) {
         status += "\n";
         status += getString(R.string.sss_incompatible_shares_warning);
         //remove the last one again, it did not fit
         shares.remove(last);
      } catch (BipSss.NotEnoughSharesException e) {
         status += "\n";
         if (e.needed == 1) {
            status += getString(R.string.sss_one_more_share_needed);
         } else {
            status += getString(R.string.sss_more_shares_needed, e.needed);
         }
      } catch (BipSss.InvalidContentTypeException e) {
         status += "\n";
         status += getString(R.string.sss_unrecognized_share_warning);
      }

      ((TextView) findViewById(R.id.tvStatus)).setText(status);
   }


   OnClickListener scanClickListener = new OnClickListener() {
      @Override
      public void onClick(View v) {
         ScanActivity.callMe(BipSsImportActivity.this, REQUEST_SHARE_CODE, StringHandleConfig.getShare());
      }
   };

   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (Activity.RESULT_OK == resultCode) {
         if (REQUEST_SHARE_CODE == requestCode) {
            StringHandlerActivity.ResultType type = (StringHandlerActivity.ResultType) intent.getSerializableExtra(StringHandlerActivity.RESULT_TYPE_KEY);
            if (type == StringHandlerActivity.ResultType.SHARE) {
               BipSss.Share share = StringHandlerActivity.getShare(intent);
               shares.add(share);
            }
         }
      } else {
         ScanActivity.toastScanError(resultCode, intent, this);
      }
      updateUI();
   }
}