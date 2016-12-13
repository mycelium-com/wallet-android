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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.mycelium.lt.api.model.TradeSession;
import com.mycelium.lt.api.model.TraderInfo;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.api.GetTraderInfo;
import com.mycelium.wallet.lt.api.Request;

public class ActiveTradesFragment extends Fragment {

   private MbwManager _mbwManager;
   private LocalTraderManager _ltManager;
   private TradeSessionsAdapter _tradeSessionAdapter;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View ret = Preconditions.checkNotNull(inflater.inflate(R.layout.lt_recent_trades_fragment, container, false));
      setHasOptionsMenu(true);
      ListView ordersList = (ListView) ret.findViewById(R.id.lvRecentTrades);
      ordersList.setOnItemClickListener(itemListClickListener);

      return ret;
   }

   private View findViewById(int id) {
      return getView().findViewById(id);
   }

   @Override
   public void onAttach(Activity activity) {
      _mbwManager = MbwManager.getInstance(getActivity().getApplication());
      _ltManager = _mbwManager.getLocalTraderManager();
      super.onAttach(activity);
   }

   @Override
   public void onResume() {
      _tradeSessionAdapter = new TradeSessionsAdapter(getActivity(), new ArrayList<TradeSession>());
      ListView list = (ListView) findViewById(R.id.lvRecentTrades);
      list.setAdapter(_tradeSessionAdapter);
      updateUi();
      _ltManager.subscribe(ltSubscriber);
      super.onResume();
   }

   @Override
   public void onPause() {
      _ltManager.unsubscribe(ltSubscriber);
      super.onPause();
   }

   private List<TradeSession> createTradeSessionList() {
      LocalTraderManager ltManager = _mbwManager.getLocalTraderManager();
      List<TradeSession> list = new LinkedList<TradeSession>();
      list.addAll(ltManager.getLocalTradeSessions());
      Collections.sort(list, new Comparator<TradeSession>() {

         @Override
         public int compare(TradeSession lhs, TradeSession rhs) {

            if (lhs.lastChange > rhs.lastChange) {
               return -1;
            } else if (lhs.lastChange < rhs.lastChange) {
               return 1;
            } else {
               return 0;
            }
         }
      });
      return list;
   }

   private void updateUi() {
      if (!isAdded()) {
         return;
      }

      List<TradeSession> tradeSessions = createTradeSessionList();
      if (tradeSessions.size() == 0) {
         findViewById(R.id.tvNoRecords).setVisibility(View.VISIBLE);
         findViewById(R.id.lvRecentTrades).setVisibility(View.GONE);
      } else {
         findViewById(R.id.tvNoRecords).setVisibility(View.GONE);
         findViewById(R.id.lvRecentTrades).setVisibility(View.VISIBLE);
         // ListView list = (ListView) findViewById(R.id.lvRecentTrades);
         // list.setAdapter(new TradeSessionsAdapter(getActivity(),
         // tradeSessions));
         _tradeSessionAdapter.clear();
         for (TradeSession tradeSession : tradeSessions) {
            _tradeSessionAdapter.add(tradeSession);
         }
         _tradeSessionAdapter.notifyDataSetChanged();
      }
   }

   OnItemClickListener itemListClickListener = new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> listView, final View view, int position, long id) {
         TradeActivity.callMe(ActiveTradesFragment.this.getActivity(), ((TradeSession) view.getTag()));
      }
   };

   private class TradeSessionsAdapter extends ArrayAdapter<TradeSession> {
      private Context _context;
      private Date _midnight;
      private DateFormat _dayFormat;
      private DateFormat _hourFormat;
      private Locale _locale;

      public TradeSessionsAdapter(Context context, List<TradeSession> objects) {
         super(context, R.layout.lt_active_trade_session_row, objects);
         _context = context;
         // Get the time at last midnight
         Calendar midnight = Calendar.getInstance();
         midnight.set(midnight.get(Calendar.YEAR), midnight.get(Calendar.MONTH), midnight.get(Calendar.DAY_OF_MONTH),
               0, 0, 0);
         _midnight = midnight.getTime();
         // Create date formats for hourly and day format
         Locale locale = getResources().getConfiguration().locale;
         _dayFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale);
         _hourFormat = android.text.format.DateFormat.getTimeFormat(_context);
         _context = context;
         _locale = new Locale("en", "US");
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
         View v = convertView;

         if (v == null) {
            LayoutInflater vi = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = Preconditions.checkNotNull(vi.inflate(R.layout.lt_active_trade_session_row, null));
         }
         TradeSession o = getItem(position);

         // Dot
         boolean viewed = _mbwManager.getLocalTraderManager().isViewed(o);
         v.findViewById(R.id.ivDot).setVisibility(viewed ? View.INVISIBLE : View.VISIBLE);

         // Peer
         String peerName = o.isOwner ? o.peerName : o.ownerName;
         ((TextView) v.findViewById(R.id.tvPeer)).setText(peerName);

         // Fiat
         String fiat = String.format(_locale, "%d %s", o.fiatTraded, o.currency);
         ((TextView) v.findViewById(R.id.tvFiat)).setText(fiat);

         // Set Date
         Date date = new Date(o.lastChange);
         DateFormat dateFormat = date.before(_midnight) ? _dayFormat : _hourFormat;
         TextView tvDate = (TextView) v.findViewById(R.id.tvDate);
         tvDate.setText(dateFormat.format(date));
         ((TextView) v.findViewById(R.id.tvDate)).setText(dateFormat.format(date));

         // Summary
         String summary;
         if (o.isBuyer) {
            summary = getResources().getString(R.string.lt_buying_details,
                  _mbwManager.getBtcValueString(o.satoshisForBuyer));
         } else {
            summary = getResources().getString(R.string.lt_selling_details,
                  _mbwManager.getBtcValueString(o.satoshisFromSeller));
         }
         ((TextView) v.findViewById(R.id.tvSummary)).setText(summary);

         // Status
         ((TextView) v.findViewById(R.id.tvStatus)).setText(o.statusText);
         v.setTag(o);
         return v;
      }
   }

   private LocalTraderEventSubscriber ltSubscriber = new LocalTraderEventSubscriber(new Handler()) {

      @Override
      public void onLtError(int errorCode) {
      }

      public void onLtSendingRequest(Request request) {
         if (!isAdded()) {
            return;
         }
         if (request instanceof GetTraderInfo) {
            // Show spinner
            findViewById(R.id.pbWait).setVisibility(View.VISIBLE);
         }
      }

      @Override
      public void onLtTraderInfoFetched(TraderInfo info, GetTraderInfo request) {
         if (!isAdded()) {
            return;
         }
         updateUi();
         // Hide spinner
         findViewById(R.id.pbWait).setVisibility(View.GONE);
      }
   };

}
