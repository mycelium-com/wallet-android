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

package com.mycelium.wallet.trezor.activity;

import android.app.Fragment;
import android.view.*;
import android.widget.*;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.HdAccountSelectorActivity;
import com.mycelium.wallet.activity.MasterseedPasswordDialog;
import com.mycelium.wapi.wallet.AccountScanManager;
import com.mycelium.wallet.trezor.TrezorManager;
import com.mycelium.wallet.activity.util.MasterseedPasswordSetter;
import com.mycelium.wallet.activity.util.AbstractAccountScanManager;

public abstract class TrezorAccountSelectorActivity extends HdAccountSelectorActivity implements TrezorManager.Events, MasterseedPasswordSetter {

   @Override
   protected AbstractAccountScanManager initMasterseedManager() {
      return MbwManager.getInstance(this).getTrezorManager();
   }

   @Override
   protected void onStart() {
      super.onStart();
      updateUi();
   }

   abstract protected AdapterView.OnItemClickListener accountClickListener();

   abstract protected void setView();

   @Override
   protected void onStop() {
      super.onStop();
      masterseedScanManager.setEventHandler(null);
   }

   @Override
   public void finish() {
      super.finish();
      masterseedScanManager.stopBackgroundAccountScan();
   }

   @Override
   public void onStatusChanged(TrezorManager.Status state, TrezorManager.AccountStatus accountState) {
      updateUi();
   }

   @Override
   protected void updateUi() {
      if (masterseedScanManager.currentState != TrezorManager.Status.initializing) {
         findViewById(R.id.tvWaitForTrezor).setVisibility(View.GONE);
         findViewById(R.id.ivConnectTrezor).setVisibility(View.GONE);
         txtStatus.setText(getString(R.string.trezor_scanning_status));
      }else{
         super.updateUi();
      }

      if (masterseedScanManager.currentAccountState == TrezorManager.AccountStatus.scanning) {
         findViewById(R.id.llStatus).setVisibility(View.VISIBLE);
         if (accounts.size()>0) {
            super.updateUi();
         }else{
            txtStatus.setText(getString(R.string.trezor_scanning_status));
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

         // Show the label and version of the connected Trezor
         findViewById(R.id.llTrezorInfo).setVisibility(View.VISIBLE);
         TrezorManager trezor = (TrezorManager) masterseedScanManager;
         ((TextView)findViewById(R.id.tvTrezorName)).setText(trezor.getFeatures().getLabel());

         String version = String.format("%s, V%d.%d", trezor.getFeatures().getDeviceId(), trezor.getFeatures().getMajorVersion(), trezor.getFeatures().getMinorVersion());
         ((TextView)findViewById(R.id.tvTrezorSerial)).setText(version);
      }

      accountsAdapter.notifyDataSetChanged();
   }

   @Override
   public void onButtonRequest() {   }

   @Override
   public String onPinMatrixRequest() {
      // PIN should never requested here - only for signing.
      throw new RuntimeException("Unexpected PIN Request");
   }

   @Override
   public void onPassphraseRequest() {
      MasterseedPasswordDialog pwd = new MasterseedPasswordDialog();
      pwd.show(getFragmentManager(), PASSPHRASE_FRAGMENT_TAG);
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


}


