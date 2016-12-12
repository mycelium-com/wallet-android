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

package com.mycelium.wallet.lt.activity;

import java.math.BigDecimal;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.common.base.Preconditions;
import com.mycelium.lt.api.model.PublicTraderInfo;
import com.mycelium.lt.api.model.TraderInfo;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.lt.LtAndroidUtils;
import com.mycelium.wallet.lt.activity.TraderInfoAdapter.InfoItem;

public class TraderInfoFragment extends Fragment {
   private PublicTraderInfo _traderInfo;
   private MbwManager _mbwManager;
   private TraderInfoAdapter _adapter;

   public static TraderInfoFragment createInstance(PublicTraderInfo traderInfo) {
      TraderInfoFragment tif = new TraderInfoFragment();
      Bundle args = new Bundle();
      args.putSerializable("traderInfo", traderInfo);
      tif.setArguments(args);
      return tif;
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View view = Preconditions.checkNotNull(inflater.inflate(R.layout.lt_trader_info_fragment, container, false));
      // may be null
      _traderInfo = (PublicTraderInfo) getArguments().getSerializable("traderInfo");
      return view;
   }

   private View findViewById(int id) {
      return getView().findViewById(id);
   }

   @Override
   public void onAttach(Activity activity) {
      _mbwManager = MbwManager.getInstance(getActivity().getApplication());
      super.onAttach(activity);
   }

   @Override
   public void onResume() {
      _adapter = new TraderInfoAdapter(getActivity(), new ArrayList<TraderInfoAdapter.InfoItem>());
      ListView list = (ListView) findViewById(R.id.lvTraderInfo);
      list.setAdapter(_adapter);
      updateUi();
      super.onResume();
   }

   private void updateUi() {
      if (!isAdded()) {
         return;
      }
      PublicTraderInfo info = _traderInfo;
      populateTraderInfo(info);
   }

   private void populateTraderInfo(PublicTraderInfo pti) {
      TraderInfo ti = null;

      _adapter.clear();
      if (pti == null) {
         return;
      }

      if (pti instanceof TraderInfo) {
         // We also have the non public info about this trader
         ti = (TraderInfo) pti;
      }

      // Show trader name
      _adapter.add(new InfoItem(getString(R.string.lt_trader_name_label), pti.nickname));

      // Show trader address
      _adapter.add(new InfoItem(getString(R.string.lt_trader_address_label), pti.address.getShortAddress()));

      // (PrivateInfo) eMail Address
      if (ti != null) {
         _adapter.add(new InfoItem(getString(R.string.lt_trader_email_address_label), ti.notificationEmail));
      }

      // Show trader last activity
      _adapter.add(new InfoItem(getString(R.string.lt_trader_last_activity), LtAndroidUtils.getTimeSpanString(this.getActivity(), pti.idleTime)));
      
      // Show trader age
      _adapter.add(new InfoItem(getString(R.string.lt_trader_age_label), getResources().getString(
            R.string.lt_time_in_days, Long.toString(pti.traderAgeMs / Constants.MS_PR_DAY))));

      // Successful Sells
      _adapter.add(new InfoItem(getString(R.string.lt_successful_sells_label), Integer.toString(pti.successfulSales)));

      // Aborted Sells
      _adapter.add(new InfoItem(getString(R.string.lt_aborted_sells_label), Integer.toString(pti.abortedSales)));

      // (PrivateInfo) Sold Volume
      if (ti != null) {
         _adapter.add(new InfoItem(getString(R.string.lt_total_sold_label), _mbwManager
               .getBtcValueString(ti.totalBtcSold)));
      }

      // Successful Buys
      _adapter.add(new InfoItem(getString(R.string.lt_successful_buys_label), Integer.toString(pti.successfulBuys)));

      // Aborted Buys
      _adapter.add(new InfoItem(getString(R.string.lt_aborted_buys_label), Integer.toString(pti.abortedBuys)));

      // (PrivateInfo) Bought Volume
      if (ti != null) {
         _adapter.add(new InfoItem(getString(R.string.lt_total_bought_label), _mbwManager
               .getBtcValueString(ti.totalBtcBought)));
      }

      // Median trade time
      if (pti.tradeMedianMs != null) {
         String hourString = LtAndroidUtils.getApproximateTimeInHours(getActivity(), pti.tradeMedianMs);
         _adapter.add(new InfoItem(getString(R.string.lt_expected_trade_time_label), hourString));
      }

      // (PrivateInfo) Local Trader Commission
      if (ti != null) {
         _adapter.add(new InfoItem(getString(R.string.lt_local_trader_commission_label), roundDoubleHalfUp(
               ti.localTraderPremium, 2).toString()
               + "%"));
      }
   }

   private static Double roundDoubleHalfUp(double value, int decimals) {
      return BigDecimal.valueOf(value).setScale(decimals, BigDecimal.ROUND_HALF_UP).doubleValue();
   }
}
