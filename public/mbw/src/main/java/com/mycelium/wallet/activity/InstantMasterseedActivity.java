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
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import com.mrd.bitlib.crypto.Bip39;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.send.SendMainActivity;
import com.mycelium.wallet.activity.util.AbstractAccountScanManager;
import com.mycelium.wallet.activity.util.MasterseedScanManager;
import com.mycelium.wapi.wallet.WalletManager;

public class InstantMasterseedActivity extends HdAccountSelectorActivity {

   private Bip39.MasterSeed masterSeed;
   private String[] words;

   public static void callMe(Activity currentActivity, String[] masterSeedWords) {
      Intent intent = new Intent(currentActivity, InstantMasterseedActivity.class);
      intent.putExtra("words", masterSeedWords);
      currentActivity.startActivity(intent);
   }

   public static void callMe(Activity currentActivity, int requestCode, Bip39.MasterSeed masterSeed) {
      Intent intent = new Intent(currentActivity, InstantMasterseedActivity.class);
      intent.putExtra("masterseed", masterSeed);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      masterSeed = (Bip39.MasterSeed) getIntent().getSerializableExtra("masterseed");
      if (masterSeed == null){
         words = getIntent().getStringArrayExtra("words");
      }
      super.onCreate(savedInstanceState);
   }

   @Override
   public void finish() {
      super.finish();
      masterseedScanManager.stopBackgroundAccountScan();
   }


   @Override
   protected void setView() {
      setContentView(R.layout.activity_instant_masterseed);
   }

   @Override
   protected AbstractAccountScanManager initMasterseedManager() {
      WalletManager walletManager = MbwManager.getInstance(this).getWalletManager(true);
      if (walletManager.accountScanManager == null) {
         if (masterSeed != null) {
            walletManager.accountScanManager = new MasterseedScanManager(this, MbwManager.getInstance(this).getNetwork(), masterSeed);
         } else {
            // only provide the words - the manager will ask for a passphrase
            walletManager.accountScanManager = new MasterseedScanManager(this, MbwManager.getInstance(this).getNetwork(), words);
         }
      }
      return (AbstractAccountScanManager) walletManager.accountScanManager;
   }

   @Override
   protected AdapterView.OnItemClickListener accountClickListener() {
      return new AdapterView.OnItemClickListener() {
         @Override
         public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            HdAccountWrapper item = (HdAccountWrapper) adapterView.getItemAtPosition(i);
            Intent intent = SendMainActivity.getIntent(InstantMasterseedActivity.this, item.id, true);
            InstantMasterseedActivity.this.startActivityForResult(intent, REQUEST_SEND);
         }
      };
   }

}
