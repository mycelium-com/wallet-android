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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.mycelium.wallet.*;
import com.mycelium.wallet.activity.modern.RecordRowBuilder;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.WalletAccount;

import java.util.List;

public class GetSpendingRecordActivity extends Activity {

   private BitcoinUri _uri;
   private MbwManager _mbwManager;

   public static void callMeWithResult(Activity currentActivity, BitcoinUri uri, int request) {
      Intent intent = new Intent(currentActivity, GetSpendingRecordActivity.class);
      intent.putExtra("uri", uri);
      currentActivity.startActivityForResult(intent, request);
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.get_spending_record_activity);
      ((ListView) findViewById(R.id.lvRecords)).setOnItemClickListener(new RecordClicked());
      _mbwManager = MbwManager.getInstance(this.getApplication());

      // Get intent parameters
      _uri = (BitcoinUri) getIntent().getSerializableExtra("uri");
   }

   class RecordClicked implements OnItemClickListener {

      @Override
      public void onItemClick(AdapterView<?> list, View v, int position, long id) {
         if (v.getTag() == null || !(v.getTag() instanceof WalletAccount)) {
            return;
         }
         WalletAccount account = (WalletAccount) v.getTag();
         SendInitializationActivity.callMe(GetSpendingRecordActivity.this, account.getId(), _uri, false);
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
      ListView listView = (ListView) findViewById(R.id.lvRecords);
      MetadataStorage storage = _mbwManager.getMetadataStorage();
      //get accounts with key and positive balance
      List<WalletAccount> spendingAccounts = _mbwManager.getWalletManager(false).getSpendingAccountsWithBalance();
      if (spendingAccounts.isEmpty()) {
         //if we dont have any account with a balance, just show all accounts with priv key
         spendingAccounts = _mbwManager.getWalletManager(false).getSpendingAccounts();
      }
      //if we have no accounts to show, just display the info text
      if (spendingAccounts.isEmpty()) {
         listView.setVisibility(View.GONE);
         warningNoSpendingAccounts.setVisibility(View.VISIBLE);
      } else {
         AccountsAdapter accountsAdapter = new AccountsAdapter(this, Utils.sortAccounts(spendingAccounts, storage));
         listView.setAdapter(accountsAdapter);
         listView.setVisibility(View.VISIBLE);
         warningNoSpendingAccounts.setVisibility(View.GONE);
      }
   }

   class AccountsAdapter extends ArrayAdapter<WalletAccount> {
      private Context _context;

      public AccountsAdapter(Context context, List<WalletAccount> accounts) {
         super(context, R.layout.record_row, accounts);
         _context = context;
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
         LayoutInflater inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         WalletAccount account = getItem(position);
         RecordRowBuilder recordRowBuilder = new RecordRowBuilder(_mbwManager, getResources(), inflater);
         return recordRowBuilder.buildRecordView(parent, account,
               false, false);
      }
   }

}