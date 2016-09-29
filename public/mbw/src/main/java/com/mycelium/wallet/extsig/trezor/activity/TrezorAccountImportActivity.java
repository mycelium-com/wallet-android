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

package com.mycelium.wallet.extsig.trezor.activity;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.ImageView;
import android.widget.TextView;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.util.AbstractAccountScanManager;
import com.mycelium.wallet.extsig.common.activity.ExtSigAccountImportActivity;
import com.mycelium.wallet.extsig.keepkey.KeepKeyManager;
import com.mycelium.wapi.wallet.AccountScanManager;
import com.squareup.otto.Subscribe;


public class TrezorAccountImportActivity extends ExtSigAccountImportActivity {
   public static void callMe(Activity currentActivity, int requestCode) {
      Intent intent = new Intent(currentActivity, TrezorAccountImportActivity.class);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   @Override
   protected AbstractAccountScanManager initMasterseedManager() {
      return MbwManager.getInstance(this).getTrezorManager();
   }

   @Override
   protected void setView() {
      super.setView();
      ((ImageView)findViewById(R.id.ivConnectExtSig)).setImageResource(R.drawable.connect_trezor);
      ((TextView)findViewById(R.id.tvDeviceType)).setText(R.string.trezor_name);
   }

   @NonNull
   @Override
   protected String getFirmwareUpdateDescription() {
      return getString(R.string.trezor_new_firmware_description);
   }


   // Otto.EventBus does not traverse class hierarchy to find subscribers
   @Subscribe
   public void onPinMatrixRequest(KeepKeyManager.OnPinMatrixRequest event) {
      super.onPinMatrixRequest(event);
   }

   @Subscribe
   public void onScanError(AccountScanManager.OnScanError event) {
      super.onScanError(event);
   }

   @Subscribe
   public void onStatusChanged(AccountScanManager.OnStatusChanged event) {
      super.onStatusChanged(event);
   }

   @Subscribe
   public void onAccountFound(AccountScanManager.OnAccountFound event) {
      super.onAccountFound(event);
   }

   @Subscribe
   public void onPassphraseRequest(AccountScanManager.OnPassphraseRequest event) {
      super.onPassphraseRequest(event);
   }

}
