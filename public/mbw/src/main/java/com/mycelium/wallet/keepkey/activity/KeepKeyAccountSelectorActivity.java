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

package com.mycelium.wallet.keepkey.activity;

import android.app.Fragment;
import android.os.Handler;
import android.os.Message;
import android.view.*;
import android.widget.*;
import com.google.common.base.Strings;
import com.mycelium.wallet.*;
import com.mycelium.wallet.activity.HdAccountSelectorActivity;
import com.mycelium.wallet.activity.MasterseedPasswordDialog;
import com.mycelium.wallet.activity.util.Pin;
import com.mycelium.wallet.ledger.LedgerManager;
import com.mycelium.wapi.wallet.AccountScanManager;
import com.mycelium.wallet.keepkey.KeepKeyManager;
import com.mycelium.wallet.activity.util.MasterseedPasswordSetter;
import com.mycelium.wallet.activity.util.AbstractAccountScanManager;
import com.squareup.otto.Subscribe;

import java.util.concurrent.LinkedBlockingQueue;

public abstract class KeepKeyAccountSelectorActivity extends HdAccountSelectorActivity implements MasterseedPasswordSetter {

   @Override
   protected AbstractAccountScanManager initMasterseedManager() {
      return MbwManager.getInstance(this).getKeepKeyManager();
   }

   @Override
   protected void onStart() {
      super.onStart();
      updateUi();
   }

   abstract protected AdapterView.OnItemClickListener accountClickListener();

   abstract protected void setView();

   @Override
   public void finish() {
      super.finish();
      masterseedScanManager.stopBackgroundAccountScan();
   }

   @Override
   protected void updateUi() {
      if (masterseedScanManager.currentState == KeepKeyManager.Status.readyToScan) {
         findViewById(R.id.tvWaitForKeepKey).setVisibility(View.GONE);
         findViewById(R.id.ivConnectKeepKey).setVisibility(View.GONE);
         txtStatus.setText(getString(R.string.keepkey_scanning_status));
      }else{
         super.updateUi();
      }

      if (masterseedScanManager.currentAccountState == KeepKeyManager.AccountStatus.scanning) {
         findViewById(R.id.llStatus).setVisibility(View.VISIBLE);
         if (accounts.size()>0) {
            super.updateUi();
         }else{
            txtStatus.setText(getString(R.string.keepkey_scanning_status));
         }

      }else if (masterseedScanManager.currentAccountState == AccountScanManager.AccountStatus.done) {
         // DONE
         findViewById(R.id.llStatus).setVisibility(View.GONE);
         findViewById(R.id.llSelectAccount).setVisibility(View.VISIBLE);
         if (accounts.size()==0) {
            // no accounts found
            findViewById(R.id.tvNoAccounts).setVisibility(View.VISIBLE);
            findViewById(R.id.lvAccounts).setVisibility(View.GONE);
         } else {
            findViewById(R.id.tvNoAccounts).setVisibility(View.GONE);
            findViewById(R.id.lvAccounts).setVisibility(View.VISIBLE);
         }

         // Show the label and version of the connected KeepKey
         findViewById(R.id.llKeepKeyInfo).setVisibility(View.VISIBLE);
         KeepKeyManager keepkey = (KeepKeyManager) masterseedScanManager;

         if (keepkey.getFeatures() != null && !Strings.isNullOrEmpty(keepkey.getFeatures().getLabel())) {
            ((TextView) findViewById(R.id.tvKeepKeyName)).setText(keepkey.getFeatures().getLabel());
         }else {
            ((TextView) findViewById(R.id.tvKeepKeyName)).setText(getString(R.string.keepkey_unnamed));
         }

         String version;
         TextView tvKeepKeySerial = (TextView) findViewById(R.id.tvKeepKeySerial);
         if (keepkey.isMostRecentVersion()) {
            if (keepkey.getFeatures() != null) {
               version = String.format("%s, V%d.%d.%d",
                     keepkey.getFeatures().getDeviceId(),
                     keepkey.getFeatures().getMajorVersion(),
                     keepkey.getFeatures().getMinorVersion(),
                     keepkey.getFeatures().getPatchVersion());
            } else {
               version = "";
            }
         }else{
            version = getString(R.string.keepkey_new_firmware);
            tvKeepKeySerial.setTextColor(getResources().getColor(R.color.semidarkgreen));
            tvKeepKeySerial.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                  Utils.showSimpleMessageDialog(KeepKeyAccountSelectorActivity.this, getString(R.string.keepkey_new_firmware_description));
               }
            });
         }
         tvKeepKeySerial.setText(version);
      }

      accountsAdapter.notifyDataSetChanged();
   }


   @Override
   public void setPassphrase(String passphrase){
      masterseedScanManager.setPassphrase(passphrase);

      if (passphrase == null){
         // user choose cancel -> leave this activity
         finish();
      } else {
         // close the dialog fragment
         Fragment fragPassphrase = getFragmentManager().findFragmentByTag(PASSPHRASE_FRAGMENT_TAG);
         if (fragPassphrase != null) {
            getFragmentManager().beginTransaction().remove(fragPassphrase).commit();
         }
      }
   }


   @Subscribe
   public void onPinMatrixRequest(KeepKeyManager.OnPinMatrixRequest event){
      KeepKeyPinDialog pin = new KeepKeyPinDialog(KeepKeyAccountSelectorActivity.this, true);
      pin.setOnPinValid(new PinDialog.OnPinEntered() {
         @Override
         public void pinEntered(PinDialog dialog, Pin pin) {
            ((KeepKeyManager) masterseedScanManager).enterPin(pin.getPin());
            dialog.dismiss();
         }
      });
      pin.show();

      // update the UI, as the state might have changed
      updateUi();
   }


   // Otto.EventBus does not traverse class hierarchy to find subscribers
   @Subscribe
   public void onScanError(AccountScanManager.OnScanError event){
      super.onScanError(event);
   }

   @Subscribe
   public void onStatusChanged(AccountScanManager.OnStatusChanged event){
      super.onStatusChanged(event);
   }

   @Subscribe
   public void onAccountFound(AccountScanManager.OnAccountFound event){
      super.onAccountFound(event);
   }

   @Subscribe
   public void onPassphraseRequest(AccountScanManager.OnPassphraseRequest event){
      super.onPassphraseRequest(event);
   }

}


