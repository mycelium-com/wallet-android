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
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.util.AddressLabel;

import java.util.Map;

public class CreateKeyActivity extends Activity {
   private MbwManager manager;
   private InMemoryPrivateKey key;

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      manager = MbwManager.getInstance(getApplication());
      setContentView(R.layout.create_key_activity);

      findViewById(R.id.btShuffle).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            createNewKey();
         }

      });

      findViewById(R.id.btUse).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            Intent result = new Intent();
            result.putExtra("base58key", key.getBase58EncodedPrivateKey(manager.getNetwork()));
            CreateKeyActivity.this.setResult(RESULT_OK, result);
            CreateKeyActivity.this.finish();
         }

      });

      createNewKey();
   }

   private void createNewKey() {
      findViewById(R.id.btUse).setEnabled(false);
      findViewById(R.id.btShuffle).setEnabled(false);

      new AsyncTask<Void, Void, InMemoryPrivateKey>() {
         @Override
         protected InMemoryPrivateKey doInBackground(Void... voids) {
            return new InMemoryPrivateKey(manager.getRandomSource(), true);
         }

         @Override
         protected void onPostExecute(InMemoryPrivateKey pk) {
            key = pk;
            Map<AddressType, Address> addresses = key.getPublicKey().getAllSupportedAddresses(manager.getNetwork());
            ((AddressLabel) findViewById(R.id.tvAddressP2PKH)).setAddress(addresses.get(AddressType.P2PKH));
            ((AddressLabel) findViewById(R.id.tvAddressP2SH)).setAddress(addresses.get(AddressType.P2SH_P2WPKH));
            ((AddressLabel) findViewById(R.id.tvAddressBech)).setAddress(addresses.get(AddressType.P2WPKH));
            findViewById(R.id.btShuffle).setEnabled(true);
            findViewById(R.id.btUse).setEnabled(true);
         }
      }.execute();
   }
}