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

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.google.common.base.Preconditions;
import com.mycelium.lt.api.model.TraderInfo;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.api.DeleteTrader;
import com.mycelium.wallet.lt.api.GetTraderInfo;
import com.mycelium.wallet.lt.api.Request;

public class MyInfoFragment extends Fragment {
   protected static final int CREATE_TRADER_RESULT_CODE = 0;
   private LocalTraderManager _ltManager;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View ret = Preconditions.checkNotNull(inflater.inflate(R.layout.lt_my_info_fragment, container, false));

      ret.findViewById(R.id.btCreate).setOnClickListener(createClickListener);
      return ret;
   }

   OnClickListener createClickListener = new OnClickListener() {

      @Override
      public void onClick(View v) {
         CreateTrader1Activity.callMe(getActivity(), CREATE_TRADER_RESULT_CODE);
      }
   };

   private View findViewById(int id) {
      return getView().findViewById(id);
   }

   @Override
   public void onAttach(Activity activity) {
      MbwManager _mbwManager = MbwManager.getInstance(getActivity().getApplication());
      _ltManager = _mbwManager.getLocalTraderManager();
      super.onAttach(activity);
   }

   @Override
   public void onResume() {
      updateUi();
      _ltManager.subscribe(ltSubscriber);
      super.onResume();
   }

   @Override
   public void onPause() {
      _ltManager.unsubscribe(ltSubscriber);
      super.onPause();
   }

   private void updateUi() {
      if (!isAdded()) {
         return;
      }
      TraderInfo info = _ltManager.getCachedTraderInfo();
      View viTraderInfo = findViewById(R.id.flTraderInfo);
      if (!_ltManager.hasLocalTraderAccount()) {
         findViewById(R.id.svNoAccount).setVisibility(View.VISIBLE);
         findViewById(R.id.pbWait).setVisibility(View.GONE);
         viTraderInfo.setVisibility(View.GONE);
      } else if (info == null) {
         findViewById(R.id.svNoAccount).setVisibility(View.GONE);
         findViewById(R.id.pbWait).setVisibility(View.VISIBLE);
         viTraderInfo.setVisibility(View.GONE);
      } else {
         findViewById(R.id.svNoAccount).setVisibility(View.GONE);
         findViewById(R.id.pbWait).setVisibility(View.GONE);
         viTraderInfo.setVisibility(View.VISIBLE);
         FragmentTransaction ft = getFragmentManager().beginTransaction();
         ft.replace(R.id.flTraderInfo, TraderInfoFragment.createInstance(info));
         ft.commitAllowingStateLoss();
      }
   }

   private LocalTraderEventSubscriber ltSubscriber = new LocalTraderEventSubscriber(new Handler()) {

      @Override
      public void onLtError(int errorCode) {
      }

      public void onLtSendingRequest(Request request) {
         if (request instanceof GetTraderInfo) {
            // Show spinner
            findViewById(R.id.pbWait).setVisibility(View.VISIBLE);
         }
      }

      @Override
      public void onLtTraderInfoFetched(TraderInfo info, GetTraderInfo request) {
         updateUi();
      }

      @Override
       public void onLtAccountDeleted(DeleteTrader request){
          updateUi();
      }
   };

}
