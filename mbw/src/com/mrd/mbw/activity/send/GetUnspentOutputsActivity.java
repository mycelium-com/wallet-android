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

package com.mrd.mbw.activity.send;

import android.app.Activity;
import android.os.Bundle;

import com.mrd.mbw.MbwManager;
import com.mrd.mbw.R;
import com.mrd.mbw.Utils;
import com.mrd.mbw.activity.send.SendActivityHelper.SendContext;
import com.mrd.mbw.activity.send.SendActivityHelper.UnspentOutputs;
import com.mrd.mbw.api.AbstractCallbackHandler;
import com.mrd.mbw.api.AndroidAsyncApi;
import com.mrd.mbw.api.AsyncTask;
import com.mrd.mbwapi.api.ApiError;
import com.mrd.mbwapi.api.QueryUnspentOutputsResponse;

public class GetUnspentOutputsActivity extends Activity {

   private AsyncTask _task;
   private MbwManager _mbwManager;
   private SendContext _context;

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.get_unspent_outputs_activity);

      // Get intent parameters
      _context = SendActivityHelper.getSendContext(this);

      _mbwManager = MbwManager.getInstance(getApplication());

      AndroidAsyncApi api = _mbwManager.getAsyncApi();
      _task = api.getUnspentOutputs(_context.spendingRecord.address, new QueryUnspentHandler());
   }

   @Override
   protected void onDestroy() {
      cancelEverything();
      super.onDestroy();
   }

   private void cancelEverything() {
      if (_task != null) {
         _task.cancel();
      }
   }

   class QueryUnspentHandler implements AbstractCallbackHandler<QueryUnspentOutputsResponse> {

      @Override
      public void handleCallback(QueryUnspentOutputsResponse response, ApiError exception) {
         Activity me = GetUnspentOutputsActivity.this;
         if (exception != null) {
            Utils.toastConnectionError(me);
            _task = null;
            me.finish();
         } else {
            UnspentOutputs outputs = new UnspentOutputs(response.unspent, response.change, response.receiving);
            SendActivityHelper.startNextActivity(me, outputs);
         }
      }
   }

}