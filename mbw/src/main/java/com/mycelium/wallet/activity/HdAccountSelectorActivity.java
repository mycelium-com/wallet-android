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
import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.common.collect.Iterables;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.model.hdpath.HdKeyPath;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wapi.wallet.AccountScanManager;
import com.mycelium.wallet.activity.util.MasterseedPasswordSetter;
import com.mycelium.wallet.activity.util.AbstractAccountScanManager;
import com.mycelium.wapi.model.Balance;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.bip44.HDAccount;
import com.squareup.otto.Subscribe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public abstract class HdAccountSelectorActivity extends Activity implements MasterseedPasswordSetter {
   protected final static int REQUEST_SEND = 1;
   public static final String PASSPHRASE_FRAGMENT_TAG = "passphrase";
   protected ArrayList<HdAccountWrapper> accounts = new ArrayList<>();
   protected AccountsAdapter accountsAdapter;
   protected AbstractAccountScanManager masterseedScanManager;



   private ListView lvAccounts;
   protected TextView txtStatus;

   protected abstract AbstractAccountScanManager initMasterseedManager();


   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      setView();

      lvAccounts = findViewById(R.id.lvAccounts);
      txtStatus = findViewById(R.id.txtStatus);

      // Accounts listview + adapter
      accountsAdapter = new AccountsAdapter(this, R.id.lvAccounts, accounts);
      lvAccounts.setAdapter(accountsAdapter);
      lvAccounts.setOnItemClickListener(accountClickListener());

      masterseedScanManager = initMasterseedManager();

      startBackgroundScan();
      updateUi();
   }

   protected void startBackgroundScan() {
      masterseedScanManager.startBackgroundAccountScan(new AccountScanManager.AccountCallback() {
         @Override
         public UUID checkForTransactions(AbstractAccountScanManager.HdKeyNodeWrapper account) {
            MbwManager mbwManager = MbwManager.getInstance(getApplicationContext());
            WalletManager walletManager = mbwManager.getWalletManager(true);

            UUID id = masterseedScanManager.createOnTheFlyAccount(
                  account.accountsRoots,
                  walletManager,
                  account.keysPaths.iterator().next().getLastIndex());

            HDAccount tempAccount = (HDAccount) walletManager.getAccount(id);
            tempAccount.doSynchronization(SyncMode.NORMAL_WITHOUT_TX_LOOKUP);

            if (tempAccount.hasHadActivity()) {
               return id;
            } else {
               tempAccount.dropCachedData();
               return null;
            }
         }
      });
   }

   abstract protected AdapterView.OnItemClickListener accountClickListener();

   abstract protected void setView();

   @Override
   public void finish() {
      super.finish();
      masterseedScanManager.stopBackgroundAccountScan();
   }

   @Override
   protected void onResume() {
      super.onResume();
      MbwManager.getEventBus().register(this);
   }

   @Override
   protected void onPause() {
      MbwManager.getEventBus().unregister(this);
      super.onPause();
   }

   protected void updateUi() {
      if (masterseedScanManager.getCurrentAccountState() == AccountScanManager.AccountStatus.scanning) {
         findViewById(R.id.llStatus).setVisibility(View.VISIBLE);
         if (accounts.size()>0) {
            txtStatus.setText(String.format(getString(R.string.account_found), Iterables.getLast(accounts).name));
            findViewById(R.id.llSelectAccount).setVisibility(View.VISIBLE);
         }
      } else if (masterseedScanManager.getCurrentAccountState() == AccountScanManager.AccountStatus.done) {
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
      }

      accountsAdapter.notifyDataSetChanged();
   }

   @Override
   public void onBackPressed() {
      super.onBackPressed();
      clearTempData();
   }

   protected void clearTempData() {
      // remove all account-data from the tempWalletManager, to improve privacy
      MbwManager.getInstance(this).forgetColdStorageWalletManager();
      masterseedScanManager.forgetAccounts();
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

   protected class HdAccountWrapper implements Serializable {
      public UUID id;
      public Collection<HdKeyPath> accountHdKeysPaths;
      public List<HdKeyNode> publicKeyNodes;
      public String name;

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         HdAccountWrapper that = (HdAccountWrapper) o;

         if (id != null ? !id.equals(that.id) : that.id != null) return false;

         return true;
      }

      @Override
      public int hashCode() {
         return id != null ? id.hashCode() : 0;
      }
   }

   protected class AccountsAdapter extends ArrayAdapter<HdAccountWrapper>{
      private LayoutInflater inflater;

      private AccountsAdapter(Context context, int resource, List<HdAccountWrapper> objects) {
         super(context, resource, objects);
         inflater = LayoutInflater.from(getContext());
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
         View row;
         if (convertView == null) {
            row = inflater.inflate(R.layout.record_row, parent, false);
         } else {
            row = convertView;
         }

         HdAccountWrapper account = getItem(position);
         ((TextView)row.findViewById(R.id.tvLabel)).setText(account.name);
         WalletAccount walletAccount = MbwManager.getInstance(getContext()).getWalletManager(true).getAccount(account.id);
         Balance balance = walletAccount.getBalance();
         String balanceString = MbwManager.getInstance(getContext()).getBtcValueString(balance.confirmed + balance.pendingChange);

         if (balance.getSendingBalance() > 0){
            balanceString += " " + String.format(getString(R.string.account_balance_sending_amount), MbwManager.getInstance(getContext()).getBtcValueString(balance.getSendingBalance()));
         }
         Drawable drawableForAccount = Utils.getDrawableForAccount(walletAccount, true, getResources());

         ((TextView)row.findViewById(R.id.tvBalance)).setText(balanceString);
         row.findViewById(R.id.tvAddress).setVisibility(View.GONE);
         ((ImageView)row.findViewById(R.id.ivIcon)).setImageDrawable(drawableForAccount);

         row.findViewById(R.id.tvProgressLayout).setVisibility(View.GONE);
         row.findViewById(R.id.tvBackupMissingWarning).setVisibility(View.GONE);
         row.findViewById(R.id.tvAccountType).setVisibility(View.GONE);

         return row;
      }
   }


   @Subscribe
   public void onScanError(AccountScanManager.OnScanError event){
      Utils.showSimpleMessageDialog(this, event.errorMessage);
   }


   @Subscribe
   public void onStatusChanged(AccountScanManager.OnStatusChanged event){
      updateUi();
   }

   @Subscribe
   public void onAccountFound(AccountScanManager.OnAccountFound event) {
      HdAccountWrapper acc = new HdAccountWrapper();
      acc.id = event.account.accountId;
      acc.accountHdKeysPaths = event.account.keysPaths;
      HdKeyPath path = event.account.keysPaths.iterator().next();
      if (path.equals(HdKeyPath.BIP32_ROOT)) {
         acc.name = getString(R.string.bip32_root_account);
      } else {
         acc.name = String.format(getString(R.string.account_number), path.getLastIndex() + 1);
      }
      acc.publicKeyNodes = event.account.accountsRoots;
      if (!accounts.contains(acc)) {
         accountsAdapter.add(acc);
         updateUi();
      }
   }

   @Subscribe
   public void onPassphraseRequest(AccountScanManager.OnPassphraseRequest event){
      MasterseedPasswordDialog pwd = new MasterseedPasswordDialog();
      pwd.show(getFragmentManager(), PASSPHRASE_FRAGMENT_TAG);
   }

}


