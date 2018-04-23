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

package com.mycelium.wallet.activity.pop;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.spvmodule.providers.TransactionContract;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.WalletApplication;
import com.mycelium.wallet.activity.main.adapter.TransactionArrayAdapter;
import com.mycelium.wallet.pop.PopRequest;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.ConfirmationRiskProfileLocal;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.bip44.Bip44BCHAccount;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;
import com.mycelium.wapi.wallet.single.SingleAddressBCHAccount;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Proof Of Payment Transaction selection Activity. Depending on the pop url, the Activity shows a
 * filtered list of outgoing transactions that match the filter in the first tab and all outgoing
 * transactions in the second.
 */
public class PopSelectTransactionActivity extends AppCompatActivity implements ActionBar.TabListener {
   private PopRequest popRequest;
   private ViewPager viewPager;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.pop_select_transaction_activity);

      PopRequest popRequest = (PopRequest) getIntent().getSerializableExtra("popRequest");
      if (popRequest == null) {
         finish();
      }
      this.popRequest = popRequest;


      MbwManager mbwManager = MbwManager.getInstance(getApplicationContext());
      WalletAccount account = mbwManager.getSelectedAccount();
      if (account.isArchived()) {
         return;
      }
      // Set up the action bar.
      final ActionBar actionBar = getSupportActionBar();
      actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

      viewPager = (ViewPager) findViewById(R.id.pager);
      HistoryPagerAdapter pagerAdapter = new HistoryPagerAdapter(getSupportFragmentManager());
      viewPager.setAdapter(pagerAdapter);

      // When swiping between different sections, select the corresponding
      // tab. We can also use ActionBar.Tab#select() to do this if we have
      // a reference to the Tab.
      viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
         @Override
         public void onPageSelected(int position) {
            actionBar.setSelectedNavigationItem(position);
            // Hide the keyboard.
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(viewPager.getWindowToken(), 0);
         }
      });


      actionBar.addTab(
            actionBar.newTab()
                  .setText(getString(R.string.pop_matching_transactions).toUpperCase())
                  .setTabListener(this));

      actionBar.addTab(
            actionBar.newTab()
                  .setText(getString(R.string.pop_non_matching_transactions).toUpperCase())
                  .setTabListener(this));

   }

   @Override
   public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
      // When the given tab is selected, switch to the corresponding page in
      // the ViewPager.
      viewPager.setCurrentItem(tab.getPosition());
   }

   @Override
   public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
   }

   @Override
   public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
   }

   public class HistoryPagerAdapter extends FragmentPagerAdapter {
      HistoryPagerAdapter(FragmentManager fm) {
         super(fm);
      }

      @Override
      public Fragment getItem(int i) {
         switch (i) {
            case 0:
               return TransactionListFragment.init(popRequest, true);
            case 1:
               return TransactionListFragment.init(popRequest, false);
            default:
               throw new RuntimeException("Unknown fragment id " + i);
         }
      }

      @Override
      public int getCount() {
         return 2;
      }

      /*@Override
      public CharSequence getPageTitle(int position) {
         return getString(position == 0 ? R.string.pop_matching_transactions : R.string.pop_non_matching_transactions);
      }*/
   }

   public static class TransactionListFragment extends ListFragment {
      private PopRequest popRequest;
      private TransactionHistoryAdapter transactionHistoryAdapter;

      static TransactionListFragment init(PopRequest popRequest, boolean showMatching) {
         TransactionListFragment list = new TransactionListFragment();

         Bundle args = new Bundle();
         args.putSerializable("pop", popRequest);
         args.putBoolean("match", showMatching);
         list.setArguments(args);

         return list;
      }

      @Override
      public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         popRequest = (PopRequest) getArguments().getSerializable("pop");
         boolean showMatching = getArguments().getBoolean("match");

         MbwManager mbwManager = MbwManager.getInstance(getActivity());
         WalletAccount account = mbwManager.getSelectedAccount();

         List<TransactionSummary> history;
         if (account.getClass() == Bip44BCHAccount.class
             || account.getClass() == SingleAddressBCHAccount.class) {
            history = getTransactions(0, 1000);
         } else  {
            history = account.getTransactionHistory(0, 1000);
         }         List<TransactionSummary> list = new ArrayList<TransactionSummary>();

         for (TransactionSummary transactionSummary : history) {
            if (transactionSummary.isIncoming) {
               // We are only interested in payments
               continue;
            }
            if (PopUtils.matches(popRequest, mbwManager.getMetadataStorage(), transactionSummary) == showMatching) {
               list.add(transactionSummary);
            }
         }

         Map<Address, String> addressBook = mbwManager.getMetadataStorage().getAllAddressLabels();
         transactionHistoryAdapter = new TransactionHistoryAdapter(getActivity(), list, addressBook);
      }

      private List<TransactionSummary> getTransactions(int offset, int limit) {
         List<TransactionSummary> transactionSummaryList = new ArrayList<>();
         WalletAccount account = MbwManager.getInstance(getContext()).getSelectedAccount();
         FragmentActivity context = getActivity();
         Uri uri = TransactionContract.TransactionSummary.CONTENT_URI(
             WalletApplication.getSpvModuleName(account.getType()));
         String selection = null;
         String[] selectionArgs = null;
         if (account instanceof Bip44BCHAccount) {
            selection = TransactionContract.TransactionSummary.SELECTION_ACCOUNT_INDEX;
            int accountIndex = ((Bip44BCHAccount) MbwManager.getInstance(getContext()).getSelectedAccount()).getAccountIndex();
            selectionArgs = new String[]{Integer.toString(accountIndex)};
         }

         if (account instanceof SingleAddressBCHAccount) {
            selection = TransactionContract.TransactionSummary.SELECTION_SINGLE_ADDRESS_ACCOUNT_GUID;
            selectionArgs = new String[]{account.getId().toString()};
         }

         Cursor cursor = null;
         ContentResolver contentResolver = context.getContentResolver();
         try {
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null);
            if (cursor != null) {
               int x = 0;
               int counter = 0;
               while (cursor.moveToNext()) {
                  if(x >= offset && counter < limit) {
                     TransactionSummary transactionSummary = from(cursor);
                     transactionSummaryList.add(transactionSummary);
                     counter++;
                  }
                  x++;
               }
            }
         } finally {
            if (cursor != null) {
               cursor.close();
            }
         }
         return transactionSummaryList;
      }

      private TransactionSummary from(Cursor cursor) {
         String rawTxId = cursor.getString(cursor.getColumnIndex(TransactionContract.TransactionSummary._ID));
         Sha256Hash txId = Sha256Hash.fromString(rawTxId);
         String rawValue = cursor.getString(cursor.getColumnIndex(TransactionContract.TransactionSummary.VALUE));
         CurrencyValue value = ExactCurrencyValue.from(new BigDecimal(rawValue), "BCH");
         int rawIsIncoming = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.IS_INCOMING));
         boolean isIncoming = rawIsIncoming == 1;
         long time = cursor.getLong(cursor.getColumnIndex(TransactionContract.TransactionSummary.TIME));
         int height = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.HEIGHT));
         int confirmations = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.CONFIRMATIONS));
         int rawIsQueuedOutgoing = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.IS_QUEUED_OUTGOING));
         boolean isQueuedOutgoing = rawIsQueuedOutgoing == 1;

         ConfirmationRiskProfileLocal confirmationRiskProfile = null;
         int unconfirmedChainLength = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.CONFIRMATION_RISK_PROFILE_LENGTH));
         if (unconfirmedChainLength > -1) {
            boolean hasRbfRisk = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.CONFIRMATION_RISK_PROFILE_LENGTH)) == 1;
            boolean isDoubleSpend = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.CONFIRMATION_RISK_PROFILE_LENGTH)) == 1;
            confirmationRiskProfile = new ConfirmationRiskProfileLocal(unconfirmedChainLength, hasRbfRisk, isDoubleSpend);
         }

         String rawDestinationAddress = cursor.getString(cursor.getColumnIndex(TransactionContract.TransactionSummary.DESTINATION_ADDRESS));
         Optional<Address> destinationAddress = Optional.absent();
         if (!TextUtils.isEmpty(rawDestinationAddress)) {
            destinationAddress = Optional.of(com.mrd.bitlib.model.Address.fromString(rawDestinationAddress));
         }
         List<com.mrd.bitlib.model.Address> toAddresses = new ArrayList<>();
         String rawToAddresses = cursor.getString(cursor.getColumnIndex(TransactionContract.TransactionSummary.TO_ADDRESSES));
         if (!TextUtils.isEmpty(rawToAddresses)) {
            String[] addresses = rawToAddresses.split(",");
            for (String addr : addresses) {
               toAddresses.add(Address.fromString(addr));
            }
         }
         return new TransactionSummary(txId, value, isIncoming, time, height, confirmations, isQueuedOutgoing,
             confirmationRiskProfile, destinationAddress, toAddresses);
      }

      @Override
      public void onViewCreated(View view, Bundle savedInstanceState) {
         super.onViewCreated(view, savedInstanceState);
         setEmptyText(getText(R.string.pop_no_matching_transactions));
         setListShown(true);
      }

      @Override
      public void onActivityCreated(Bundle savedInstanceState) {
         super.onActivityCreated(savedInstanceState);
         setListAdapter(transactionHistoryAdapter);
      }
   }

   public static class TransactionHistoryAdapter extends TransactionArrayAdapter {
      TransactionHistoryAdapter(Context context, List<TransactionSummary> objects, Map<Address, String> addressBook) {
         super(context, objects, addressBook);
      }

      @Override
      public View getView(final int position, View convertView, ViewGroup parent) {
         View view = super.getView(position, convertView, parent);
         view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               ((PopSelectTransactionActivity) getContext()).onTxClick(getItem(position).txid);
            }
         });
         return view;
      }
   }

   protected void onTxClick(Sha256Hash txid) {
      Intent signPopIntent = new Intent(PopSelectTransactionActivity.this, PopActivity.class);
      signPopIntent.putExtra("popRequest", popRequest);
      signPopIntent.putExtra("selectedTransactionToProve", txid);
      startActivity(signPopIntent);
      finish();
   }
}
