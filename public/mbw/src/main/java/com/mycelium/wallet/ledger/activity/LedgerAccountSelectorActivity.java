/*
 * Copyright 2015 Megion Research and Development GmbH
 * Copyright 2015 Ledger
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

package com.mycelium.wallet.ledger.activity;

import android.view.View;
import android.widget.AdapterView;
import com.mycelium.wallet.LedgerPinDialog;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.PinDialog;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.HdAccountSelectorActivity;
import com.mycelium.wallet.activity.util.AbstractAccountScanManager;
import com.mycelium.wallet.activity.util.Pin;
import com.mycelium.wallet.ledger.LedgerManager;
import com.mycelium.wapi.wallet.AccountScanManager;
import com.squareup.otto.Subscribe;


public abstract class LedgerAccountSelectorActivity extends HdAccountSelectorActivity {

   @Override
   protected AbstractAccountScanManager initMasterseedManager() {
      return MbwManager.getInstance(this).getLedgerManager();
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
      if ((masterseedScanManager.currentState != AccountScanManager.Status.initializing) &&
            (masterseedScanManager.currentState != AccountScanManager.Status.unableToScan)) {
         findViewById(R.id.tvWaitForLedger).setVisibility(View.GONE);
         findViewById(R.id.ivConnectLedger).setVisibility(View.GONE);
         txtStatus.setText(getString(R.string.ledger_scanning_status));
      } else {
         super.updateUi();
      }

      if (masterseedScanManager.currentAccountState == AccountScanManager.AccountStatus.scanning) {
         findViewById(R.id.llStatus).setVisibility(View.VISIBLE);
         if (accounts.size() > 0) {
            super.updateUi();
         } else {
            txtStatus.setText(getString(R.string.ledger_scanning_status));
         }

      } else if (masterseedScanManager.currentAccountState == AccountScanManager.AccountStatus.done) {
         // DONE
         findViewById(R.id.llStatus).setVisibility(View.GONE);
         findViewById(R.id.llSelectAccount).setVisibility(View.VISIBLE);
         if (accounts.size() == 0) {
            // no accounts found
            findViewById(R.id.tvNoAccounts).setVisibility(View.VISIBLE);
            findViewById(R.id.lvAccounts).setVisibility(View.GONE);
         } else {
            findViewById(R.id.tvNoAccounts).setVisibility(View.GONE);
            findViewById(R.id.lvAccounts).setVisibility(View.VISIBLE);
         }
      }

      accountsAdapter.notifyDataSetChanged();
   }

   @Subscribe()
   public void onPinRequest(LedgerManager.OnPinRequest event) {
      LedgerPinDialog pin = new LedgerPinDialog(this, true);
      pin.setTitle(R.string.ledger_enter_pin);
      pin.setOnPinValid(new PinDialog.OnPinEntered() {
         @Override
         public void pinEntered(PinDialog dialog, Pin pin) {
            ((LedgerManager) masterseedScanManager).enterPin(pin.getPin());
            dialog.dismiss();
         }
      });
      pin.show();
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
