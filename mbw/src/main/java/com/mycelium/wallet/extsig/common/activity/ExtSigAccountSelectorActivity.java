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

package com.mycelium.wallet.extsig.common.activity;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.support.annotation.NonNull;
import android.view.*;
import android.widget.*;
import com.google.common.base.Strings;
import com.mycelium.wallet.*;
import com.mycelium.wallet.activity.HdAccountSelectorActivity;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.util.Pin;
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager;
import com.mycelium.wapi.wallet.AccountScanManager;
import com.mycelium.wallet.activity.util.MasterseedPasswordSetter;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.bip44.HDAccount;
import com.squareup.otto.Subscribe;

public abstract class ExtSigAccountSelectorActivity extends HdAccountSelectorActivity implements MasterseedPasswordSetter {

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

   @SuppressLint("DefaultLocale") // It's only for display.
   @Override
   protected void updateUi() {

      if (masterseedScanManager.getCurrentState() == ExternalSignatureDeviceManager.Status.readyToScan) {
         findViewById(R.id.tvWaitForExtSig).setVisibility(View.GONE);
         findViewById(R.id.ivConnectExtSig).setVisibility(View.GONE);
         txtStatus.setText(getString(R.string.ext_sig_scanning_status));
      }else{
         super.updateUi();
      }

      if (masterseedScanManager.getCurrentAccountState() == ExternalSignatureDeviceManager.AccountStatus.scanning) {
         findViewById(R.id.llStatus).setVisibility(View.VISIBLE);
         if (accounts.size()>0) {
            super.updateUi();
         }else{
            txtStatus.setText(getString(R.string.ext_sig_scanning_status));
         }

      }else if (masterseedScanManager.getCurrentAccountState() == AccountScanManager.AccountStatus.done) {
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

         // Show the label and version of the connected Trezor
         findViewById(R.id.llExtSigInfo).setVisibility(View.VISIBLE);
         final ExternalSignatureDeviceManager extSigDevice = (ExternalSignatureDeviceManager) masterseedScanManager;

         if (extSigDevice.getFeatures() != null && !Strings.isNullOrEmpty(extSigDevice.getFeatures().getLabel())) {
            ((TextView) findViewById(R.id.tvExtSigName)).setText(extSigDevice.getFeatures().getLabel());
         }else {
            ((TextView) findViewById(R.id.tvExtSigName)).setText(getString(R.string.ext_sig_unnamed));
         }

         String version;
         TextView tvTrezorSerial = findViewById(R.id.tvExtSigSerial);
         if (extSigDevice.isMostRecentVersion()) {
            if (extSigDevice.getFeatures() != null) {
               version = String.format("%s, V%d.%d.%d",
                     extSigDevice.getFeatures().getDeviceId(),
                     extSigDevice.getFeatures().getMajorVersion(),
                     extSigDevice.getFeatures().getMinorVersion(),
                     extSigDevice.getFeatures().getPatchVersion());
            } else {
               version = "";
            }
         }else{
            version = getString(R.string.ext_sig_new_firmware);
            tvTrezorSerial.setTextColor(getResources().getColor(R.color.semidarkgreen));
            tvTrezorSerial.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                  if (extSigDevice.hasExternalConfigurationTool()) {
                     extSigDevice.openExternalConfigurationTool(ExtSigAccountSelectorActivity.this, getString(R.string.external_app_needed),  null);
                  } else {
                     Utils.showSimpleMessageDialog(ExtSigAccountSelectorActivity.this, getFirmwareUpdateDescription());
                  }
               }
            });
         }
         tvTrezorSerial.setText(version);
      }

      accountsAdapter.notifyDataSetChanged();
   }

   @NonNull
   abstract protected String getFirmwareUpdateDescription();



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
   public void onPinMatrixRequest(ExternalSignatureDeviceManager.OnPinMatrixRequest event){
      TrezorPinDialog pin = new TrezorPinDialog(ExtSigAccountSelectorActivity.this, true);
      pin.setOnPinValid(new PinDialog.OnPinEntered() {
         @Override
         public void pinEntered(PinDialog dialog, Pin pin) {
            ((ExternalSignatureDeviceManager) masterseedScanManager).enterPin(pin.getPin());
            dialog.dismiss();
         }
      });
      pin.show();

      // update the UI, as the state might have changed
      updateUi();
   }


   @Subscribe
   public void onScanError(final AccountScanManager.OnScanError event){
      ExternalSignatureDeviceManager extSigDevice = (ExternalSignatureDeviceManager) masterseedScanManager;
      // see if we know how to init that device
      if (event.errorType == AccountScanManager.OnScanError.ErrorType.NOT_INITIALIZED &&
              extSigDevice.hasExternalConfigurationTool()){
         extSigDevice.openExternalConfigurationTool(this, getString(R.string.ext_sig_device_not_initialized), new Runnable() {
            @Override
            public void run() {
               // close this activity and let the user restart it after the tool ran
               ExtSigAccountSelectorActivity.this.finish();
            }
         });
      } else {
         super.onScanError(event);
      }
   }

   @Subscribe
   public void onStatusChanged(AccountScanManager.OnStatusChanged event){
      super.onStatusChanged(event);
   }

   @Subscribe
   public void onAccountFound(AccountScanManager.OnAccountFound event){
      super.onAccountFound(event);
       WalletManager walletManager = MbwManager.getInstance(getApplicationContext()).getWalletManager(false);
       if (walletManager.hasAccount(event.account.accountId)) {
          boolean upgraded = masterseedScanManager.upgradeAccount(event.account.accountsRoots,
                  walletManager, event.account.accountId);
          if (upgraded) {
             // If it's migrated it's 100% that it's HD
             int accountIndex = ((HDAccount) walletManager.getAccount(event.account.accountId)).getAccountIndex();
             new Toaster(this).toast(getString(R.string.account_upgraded, accountIndex + 1), false);
          }
       }
   }

   @Subscribe
   public void onPassphraseRequest(AccountScanManager.OnPassphraseRequest event){
      super.onPassphraseRequest(event);
   }

}


