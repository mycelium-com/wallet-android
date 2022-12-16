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

package com.mycelium.wallet.activity.modern;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.modern.adapter.TabsAdapter;
import com.mycelium.wallet.activity.send.SendCoinsActivity;

public class GetFromAddressBookActivity extends AppCompatActivity {
   private static final String TAB_MY_ADDRESSES = "tab_my_addresses";
   private static final String TAB_CONTACTS = "tab_contacts";

   ViewPager mViewPager;
   TabsAdapter mTabsAdapter;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      MbwManager _mbwManager = MbwManager.getInstance(this);
      setContentView(R.layout.activity_get_from_addressbook);
      mViewPager = findViewById(R.id.pager);
      TabLayout tabLayout = findViewById(R.id.pager_tabs);
      tabLayout.setupWithViewPager(mViewPager);

      mTabsAdapter = new TabsAdapter(this, mViewPager, _mbwManager);

      TabLayout.Tab myAddressesTab = tabLayout.newTab().setText(getResources().getString(R.string.my_accounts));
      mTabsAdapter.addTab(myAddressesTab, AddressBookFragment.class,
              addressBookBundle(true, false), TAB_MY_ADDRESSES);
      TabLayout.Tab contactsTab = tabLayout.newTab().setText(getResources().getString(R.string.sending_addresses));
      mTabsAdapter.addTab(contactsTab, AddressBookFragment.class,
              addressBookBundle(false, true), TAB_CONTACTS);

      int countContactsEntries = _mbwManager.getMetadataStorage().getAllAddressLabels().size();

      if (countContactsEntries > 0) {
         contactsTab.select();
         mViewPager.setCurrentItem(mTabsAdapter.indexOf(TAB_CONTACTS));
      } else {
         myAddressesTab.select();
         mViewPager.setCurrentItem(mTabsAdapter.indexOf(TAB_MY_ADDRESSES));
      }
   }

   /**
    * Method for creating address book configuration which used in Send Activity
    * @param own need for definition necessary configuration - print addresses from our wallet or not
    * @param availableForSending need for definition necessary configuration - print only addresses available for sending or all addresses
    * @return Bundle for address book
    */
   private Bundle addressBookBundle(boolean own, boolean availableForSending) {
      final Bundle ownBundle = new Bundle();
      ownBundle.putBoolean(AddressBookFragment.OWN, own);
      ownBundle.putBoolean(AddressBookFragment.SELECT_ONLY, true);
      ownBundle.putBoolean(AddressBookFragment.AVAILABLE_FOR_SENDING, availableForSending);
      if (getIntent().hasExtra(SendCoinsActivity.ACCOUNT)) {
         ownBundle.putSerializable(SendCoinsActivity.ACCOUNT, getIntent().getSerializableExtra(SendCoinsActivity.ACCOUNT));
         ownBundle.putBoolean(SendCoinsActivity.IS_COLD_STORAGE, getIntent().getBooleanExtra(SendCoinsActivity.IS_COLD_STORAGE, false));
      }
      return ownBundle;
   }
}
