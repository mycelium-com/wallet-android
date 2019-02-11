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

package com.mycelium.wallet.activity.send;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;

import com.mrd.bitlib.crypto.BipSss;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.BipSsImportActivity;
import com.mycelium.wallet.activity.EnterWordListActivity;
import com.mycelium.wallet.activity.InstantMasterseedActivity;
import com.mycelium.wallet.activity.ScanActivity;
import com.mycelium.wallet.activity.StringHandlerActivity;
import com.mycelium.wallet.content.HandleConfigFactory;
import com.mycelium.wallet.content.ResultType;
import com.mycelium.wallet.extsig.keepkey.activity.InstantKeepKeyActivity;
import com.mycelium.wallet.extsig.trezor.activity.InstantTrezorActivity;
import com.mycelium.wapi.content.GenericAssetUri;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.btc.bip44.UnrelatedHDAccountConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getAddress;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getAssetUri;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getHdKeyNode;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getPrivateKey;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getShare;

public class InstantWalletActivity extends FragmentActivity {
   public static final int REQUEST_SCAN = 0;
   private static final int REQUEST_TREZOR = 1;
   private static final int IMPORT_WORDLIST = 2;
   private static final int REQUEST_KEEPKEY = 3;

   public static void callMe(Activity currentActivity) {
      Intent intent = new Intent(currentActivity, InstantWalletActivity.class);
      currentActivity.startActivity(intent);
   }

   /**
    * Called when the activity is first created.
    */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.instant_wallet_activity);


      findViewById(R.id.btClipboard).setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View arg0) {
            handleString(Utils.getClipboardString(InstantWalletActivity.this));
         }
      });

      findViewById(R.id.btMasterseed).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            EnterWordListActivity.callMe(InstantWalletActivity.this, IMPORT_WORDLIST, true);
         }
      });

      findViewById(R.id.btScan).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            ScanActivity.callMe(InstantWalletActivity.this, REQUEST_SCAN, HandleConfigFactory.spendFromColdStorage());
         }
      });

      findViewById(R.id.btTrezor).setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View arg0) {
            InstantTrezorActivity.callMe(InstantWalletActivity.this, REQUEST_TREZOR);
         }
      });

      findViewById(R.id.btKeepKey).setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View arg0) {
            InstantKeepKeyActivity.callMe(InstantWalletActivity.this, REQUEST_KEEPKEY);
         }
      });
   }

   private void handleString(String str) {
      Intent intent = StringHandlerActivity.getIntent(this,
              HandleConfigFactory.spendFromColdStorage(), str);
      startActivityForResult(intent, REQUEST_SCAN);
   }

   @Override
   protected void onResume() {
      super.onResume();
      StringHandlerActivity.ParseAbility canHandle = StringHandlerActivity.canHandle(
              HandleConfigFactory.spendFromColdStorage(),
            Utils.getClipboardString(this),
            MbwManager.getInstance(this).getNetwork());

      if (canHandle == StringHandlerActivity.ParseAbility.NO) {
         findViewById(R.id.btClipboard).setEnabled(false);
      } else {
         findViewById(R.id.btClipboard).setEnabled(true);
      }
   }

   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == REQUEST_SCAN) {
         if (resultCode != RESULT_OK) {
            ScanActivity.toastScanError(resultCode, intent, this);
         } else {
            MbwManager mbwManager = MbwManager.getInstance(this);
            ResultType type = (ResultType) intent.getSerializableExtra(StringHandlerActivity.RESULT_TYPE_KEY);
            switch (type) {
               case PRIVATE_KEY:
                  InMemoryPrivateKey key = getPrivateKey(intent);
                  sendWithAccount(mbwManager.createOnTheFlyAccount(key));
                  break;
               case ADDRESS:
                  GenericAddress address = getAddress(intent);
                  sendWithAccount(mbwManager.createOnTheFlyAccount(address));
                  break;
               case ASSET_URI:
                  GenericAssetUri uri = getAssetUri(intent);
                  sendWithAccount(mbwManager.createOnTheFlyAccount(uri.getAddress()));
                  break;
               case HD_NODE:
                  HdKeyNode hdKeyNode = getHdKeyNode(intent);
                  final WalletManager tempWalletManager = mbwManager.getWalletManager(true);
                  UUID account = tempWalletManager.createAccounts(new UnrelatedHDAccountConfig(Collections.singletonList(hdKeyNode))).get(0);
                  tempWalletManager.setActiveAccount(account);
                  sendWithAccount(account);
                  break;
               case SHARE:
                  BipSss.Share share = getShare(intent);
                  BipSsImportActivity.callMe(this, share, StringHandlerActivity.IMPORT_SSS_CONTENT_CODE);
                  break;
            }
         }
         // else {
         // We don't call finish() here, so that this activity stays on the back stack.
         // So the user can click back and scan the next cold storage.
         // }
      } else if (requestCode == REQUEST_TREZOR) {
         if (resultCode == RESULT_OK) {
            finish();
         }
      } else if (requestCode == REQUEST_KEEPKEY) {
         if (resultCode == RESULT_OK) {
            finish();
         }
      } else if (requestCode == IMPORT_WORDLIST) {
         if (resultCode == RESULT_OK) {
            ArrayList<String> wordList = intent.getStringArrayListExtra(EnterWordListActivity.MASTERSEED);
            String password = intent.getStringExtra(EnterWordListActivity.PASSWORD);
            InstantMasterseedActivity.callMe(this, wordList.toArray(new String[0]), password);
         }
      } else {
         throw new IllegalStateException("unknown return codes after scanning... " + requestCode + " " + resultCode);
      }
   }

   private void sendWithAccount(UUID account) {
      //we don't know yet where and what to send
      SendInitializationActivity.callMeWithResult(this, account, true,
              StringHandlerActivity.SEND_INITIALIZATION_CODE);
   }

   @Override
   public void finish() {
      // drop and create a new TempWalletManager so that no sensitive data remains in memory
      MbwManager.getInstance(this).forgetColdStorageWalletManager();
      super.finish();
   }
}
