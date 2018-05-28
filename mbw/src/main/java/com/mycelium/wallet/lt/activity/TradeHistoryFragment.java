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
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.endless.EndlessAdapter;
import com.mycelium.lt.api.model.TradeSession;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.api.GetFinalTradeSessions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.google.common.base.Preconditions.checkNotNull;

public class TradeHistoryFragment extends Fragment {
   private MbwManager _mbwManager;
   private LocalTraderManager _ltManager;
   private Wrapper _myAdapter;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View ret = checkNotNull(inflater.inflate(R.layout.lt_recent_trades_fragment, container, false));
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
      _myAdapter = new Wrapper(getActivity(), new ArrayList<TradeSession>());
      if (_ltManager.hasLocalTraderAccount()) {
         ListView list = (ListView) findViewById(R.id.lvRecentTrades);
         list.setAdapter(_myAdapter);
      }
      super.onResume();
   }

   @Override
   public void onPause() {
      if (_myAdapter != null) {
         _myAdapter.detach();
      }
      super.onPause();
   }

   private OnItemClickListener itemListClickListener = new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> listView, final View view, int position, long id) {
         TradeActivity.callMe(TradeHistoryFragment.this.getActivity(), ((TradeSession) view.getTag()));
      }
   };

   private final class TradeSessionsAdapter extends ArrayAdapter<TradeSession> {
      private Context _context;
      private Date _midnight;
      private DateFormat _dayFormat;
      private DateFormat _hourFormat;
      private Locale _locale;

      public TradeSessionsAdapter(Context context, List<TradeSession> objects) {
         super(context, R.layout.lt_historic_trade_session_row, objects);
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
            v = checkNotNull(vi.inflate(R.layout.lt_historic_trade_session_row, null));
         }
         TradeSession o = checkNotNull(getItem(position));

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

         // Status
         ((TextView) v.findViewById(R.id.tvStatus)).setText(o.statusText);
         v.setTag(o);
         return v;
      }
   }

   private class Wrapper extends EndlessAdapter {
      private static final int FETCH_LIMIT = 10;
      private RotateAnimation rotate = null;
      private final List<TradeSession> _fetched;
      private List<TradeSession> _toAdd;
      private GetFinalTradeSessions _request;

      private Wrapper(Context ctxt, List<TradeSession> list) {
         super(new TradeSessionsAdapter(ctxt, list));
         rotate = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
         rotate.setDuration(600);
         rotate.setRepeatMode(Animation.RESTART);
         rotate.setRepeatCount(Animation.INFINITE);
         _fetched = new LinkedList<TradeSession>();
         _toAdd = new LinkedList<TradeSession>();
         _ltManager.subscribe(ltSubscriber);
      }

      private LocalTraderEventSubscriber ltSubscriber = new LocalTraderEventSubscriber(new Handler()) {

         @Override
         public void onLtError(int errorCode) {
            detach();
         }

         public void onLtFinalTradeSessionsFetched(List<TradeSession> list, GetFinalTradeSessions request) {
            synchronized (_fetched) {
               if (_request != request) {
                  return;
               }
               for (TradeSession item : list) {
                  _fetched.add(item);
               }
               _fetched.notify();
            }
         }
      };

      public void detach() {
         _ltManager.unsubscribe(ltSubscriber);
         synchronized (_fetched) {
            _fetched.notify();
         }

      }

      @Override
      protected View getPendingView(ViewGroup parent) {
         return LayoutInflater.from(parent.getContext()).inflate(R.layout.lt_trade_session_row_fetching, null);
      }

      @Override
      @SuppressFBWarnings(
            justification = "looping happens anyway, but in a higher level",
            value = "WA_NOT_IN_LOOP")
      protected boolean cacheInBackground() {
         if (!_ltManager.hasLocalTraderAccount()) {
            return false;
         }
         synchronized (_fetched) {
            _fetched.clear();
            _request = new GetFinalTradeSessions(FETCH_LIMIT, getWrappedAdapter().getCount());
            _ltManager.makeRequest(_request);
            try {
               _fetched.wait();
            } catch (InterruptedException e) {
               return false;
            }
         }
         synchronized (_toAdd) {
            for (TradeSession item : _fetched) {
               _toAdd.add(item);
            }
         }

         boolean moreToFetch = _fetched.size() == FETCH_LIMIT;
         return moreToFetch;
      }

      @Override
      protected void appendCachedData() {
         synchronized (_toAdd) {
            TradeSessionsAdapter a = (TradeSessionsAdapter) getWrappedAdapter();

            for (TradeSession item : _toAdd) {
               a.add(item);
            }
            _toAdd.clear();
         }
      }

   }

}
