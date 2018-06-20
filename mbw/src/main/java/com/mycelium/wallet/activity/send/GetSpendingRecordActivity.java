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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.mycelium.wallet.BitcoinUri;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.RecordRowBuilder;
import com.mycelium.wallet.activity.modern.adapter.holder.AccountViewHolder;
import com.mycelium.wallet.activity.modern.model.ViewAccountModel;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.WalletAccount;

import java.util.ArrayList;
import java.util.List;

public class GetSpendingRecordActivity extends Activity {

   private BitcoinUri _uri;
   private MbwManager _mbwManager;
   private boolean _showAccounts = false;
   private byte[] _rawPr;

   private ListView listView;
   private AccountsAdapter accountsAdapter;
   private RecordRowBuilder builder;

   public static void callMeWithResult(Activity currentActivity, BitcoinUri uri, int request) {
      Intent intent = new Intent(currentActivity, GetSpendingRecordActivity.class);
      intent.putExtra("uri", uri);
      currentActivity.startActivityForResult(intent, request);
   }

   public static void callMeWithResult(Activity currentActivity, byte[] rawPaymentRequest, int request) {
      Intent intent = new Intent(currentActivity, GetSpendingRecordActivity.class);
      intent.putExtra("rawPr", rawPaymentRequest);
      currentActivity.startActivityForResult(intent, request);
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.get_spending_record_activity);
      _mbwManager = MbwManager.getInstance(this.getApplication());
      builder = new RecordRowBuilder(_mbwManager, getResources());
      listView = findViewById(R.id.lvRecords);
      listView.setOnItemClickListener(new RecordClicked());

      // Get intent parameters
      _uri = (BitcoinUri) getIntent().getSerializableExtra("uri");
      _rawPr = getIntent().getByteArrayExtra("rawPr");

      if (savedInstanceState != null){
         _showAccounts = savedInstanceState.getBoolean("showAccounts");
      }

      // if the app is in Locked-Mode, just pass the active account along and finish
      if (!_showAccounts && _mbwManager.isKeyManagementLocked()){
         if (_mbwManager.getSelectedAccount().canSpend()) {
            // if the current locked account canSpend, use this and go directly to sending
            callSendInitActivity(_mbwManager.getSelectedAccount());

            GetSpendingRecordActivity.this.finish();
         } else {
            // if this is a watch-only account, request the PIN to show the accounts
            _mbwManager.runPinProtectedFunction(this, new Runnable() {
               @Override
               public void run() {
                  _showAccounts = true;
                  update();
               }
            });
         }
      } else {
         _showAccounts = true;
      }

   }

   private void callSendInitActivity(WalletAccount account) {
      if (_rawPr != null){
         SendInitializationActivity.callMe(GetSpendingRecordActivity.this, account.getId(), _rawPr, false);
      } else {
         SendInitializationActivity.callMe(GetSpendingRecordActivity.this, account.getId(), _uri, false);
      }
   }


   @Override
   protected void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putBoolean("showAccounts", _showAccounts);
   }

   class RecordClicked implements OnItemClickListener {
      @Override
      public void onItemClick(AdapterView<?> list, View v, int position, long id) {
         ViewAccountModel model = accountsAdapter.getItem(position);
         WalletAccount account = _mbwManager.getWalletManager(false).getAccount(model.accountId);
         callSendInitActivity(account);
         GetSpendingRecordActivity.this.finish();
      }
   }

   @Override
   protected void onResume() {
      update();
      super.onResume();
   }

   private void update() {
      View warningNoSpendingAccounts = findViewById(R.id.tvNoSpendingAccounts);
      MetadataStorage storage = _mbwManager.getMetadataStorage();
      //get accounts with key and positive balance
      List<WalletAccount> spendingAccounts = _mbwManager.getWalletManager(false).getSpendingAccountsWithBalance();
      if (spendingAccounts.isEmpty()) {
         //if we dont have any account with a balance, just show all accounts with priv key
         spendingAccounts = _mbwManager.getWalletManager(false).getSpendingAccounts();
      }
      ArrayList<WalletAccount> result = new ArrayList<>();
      for (WalletAccount spendingAccount : spendingAccounts) {
         if(spendingAccount.getCurrencyBasedBalance().confirmed.isBtc()) {
            result.add(spendingAccount);
         }
      }
      spendingAccounts = result;

      //if we have no accounts to show, just display the info text
      if (!_showAccounts || spendingAccounts.isEmpty()) {
         listView.setVisibility(View.GONE);
         warningNoSpendingAccounts.setVisibility(View.VISIBLE);
      } else {
         List<ViewAccountModel> list = builder.convertList(Utils.sortAccounts(spendingAccounts, storage));
         accountsAdapter = new AccountsAdapter(this, list);
         listView.setAdapter(accountsAdapter);
         listView.setVisibility(View.VISIBLE);
         warningNoSpendingAccounts.setVisibility(View.GONE);
      }
   }

   class AccountsAdapter extends ArrayAdapter<ViewAccountModel> {
      private LayoutInflater inflater;

      AccountsAdapter(Context context, List<ViewAccountModel> accounts) {
         super(context, R.layout.record_row, accounts);
         inflater = LayoutInflater.from(context);
      }

       @Override
       @NonNull
       public View getView(int position, View convertView, @NonNull ViewGroup parent) {
           ViewAccountModel account = getItem(position);
           if (convertView == null) {
               convertView = inflater.inflate(R.layout.record_row, null, false);
               convertView.setTag(new AccountViewHolder(convertView));
           }
           RecordRowBuilder recordRowBuilder = new RecordRowBuilder(_mbwManager, getResources());
           recordRowBuilder.buildRecordView((AccountViewHolder) convertView.getTag(), account, false, false);
           return convertView;
       }
   }
}