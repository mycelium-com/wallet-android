/*
 * Copyright 2013 Megion Research and Development GmbH
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

package com.mycelium.wallet.lt.activity.buy;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.mycelium.lt.api.model.GpsLocation;
import com.mycelium.lt.api.model.SellOrderSearchItem;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.LtAndroidUtils;
import com.mycelium.wallet.lt.activity.ChangeLocationActivity;
import com.mycelium.wallet.lt.activity.SendRequestActivity;
import com.mycelium.wallet.lt.api.GetSellOrder;
import com.mycelium.wallet.lt.api.SellOrderSearch;

public class SellOrderSearchFragment extends Fragment {

   public static final int MAX_SEARCH_RESULTS = 30;
   private static final String SELL_ORDERS = "sellOrders";
   private MbwManager _mbwManager;
   private LocalTraderManager _ltManager;
   private ActionMode currentActionMode;
   private List<SellOrderSearchItem> _sellOrders;
   private SellOrderAdapter _recordsAdapter;
   private SellOrderSearchItem _selected; // todo rework this so that the
                                          // listview does not get redrawn

   @SuppressWarnings("unchecked")
   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View view = Preconditions
            .checkNotNull(inflater.inflate(R.layout.lt_sell_order_search_fragment, container, false));
      setHasOptionsMenu(true);
      if (savedInstanceState != null) {
         // May be null
         _sellOrders = (List<SellOrderSearchItem>) savedInstanceState.getSerializable(SELL_ORDERS);
      }
      ((Button) view.findViewById(R.id.btChange)).setOnClickListener(changeLocationClickListener);
      return view;
   }

   private OnClickListener changeLocationClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         // Change the current location
         ChangeLocationActivity.callMe(SellOrderSearchFragment.this.getActivity());
      }
   };

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
   public void onDetach() {
      super.onDetach();
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
   }

   @Override
   public void setUserVisibleHint(boolean isVisibleToUser) {
      super.setUserVisibleHint(isVisibleToUser);
      if (!isVisibleToUser) {
         finishActionMode();
      }
   }

   private void finishActionMode() {
      if (currentActionMode != null) {
         currentActionMode.finish();
      }
   }

   @Override
   public void onResume() {
      _ltManager.subscribe(ltSubscriber);
      GpsLocation location = _ltManager.getUserLocation();
      _ltManager.makeRequest(new SellOrderSearch(location, SellOrderSearchFragment.MAX_SEARCH_RESULTS));
      updateUi(true);
      super.onResume();
   }

   @Override
   public void onPause() {
      _ltManager.unsubscribe(ltSubscriber);
      super.onPause();
   }

   @Override
   public void onSaveInstanceState(Bundle outState) {
      outState.putSerializable(SELL_ORDERS, (Serializable) _sellOrders);
   }

   private void updateUi(boolean updating) {
      if (!isAdded()) {
         return;
      }

      // show / hide location
      findViewById(R.id.llLocation).setVisibility(View.VISIBLE);

      ((TextView) findViewById(R.id.tvLocation)).setText(_ltManager.getUserLocation().name);

      if (_sellOrders == null) {
         findViewById(R.id.pbWait).setVisibility(View.VISIBLE);
         findViewById(R.id.tvSearching).setVisibility(View.VISIBLE);
         findViewById(R.id.lvRecords).setVisibility(View.GONE);
      } else {
         findViewById(R.id.pbWait).setVisibility(View.GONE);
         findViewById(R.id.tvSearching).setVisibility(View.GONE);
         findViewById(R.id.lvRecords).setVisibility(View.VISIBLE);
         _recordsAdapter = new SellOrderAdapter(getActivity(), _sellOrders);
         ListView listView = (ListView) findViewById(R.id.lvRecords);
         listView.setAdapter(_recordsAdapter);
      }
   }

   private class SellOrderAdapter extends ArrayAdapter<SellOrderSearchItem> {

      private class Tag {
         private SellOrderSearchItem item;
         private int position;

         public Tag(SellOrderSearchItem item, int position) {
            this.item = item;
            this.position = position;
         }
      }

      private static final int METERS_PR_MILE = 1609;
      private static final long MS_PER_DAY = 1000 * 60 * 60 * 24;
      private Locale _locale;
      private Context _context;
      private boolean _useKilometers;

      public SellOrderAdapter(Context context, List<SellOrderSearchItem> objects) {
         super(context, R.layout.lt_sell_order_card, objects);
         _locale = new Locale("en", "US");
         _context = context;
         // XXX we need to determine whether to use miles or kilometers
         _useKilometers = true;
      }

      @Override
      public View getView(final int position, View convertView, ViewGroup parent) {

         final SellOrderSearchItem orderItem = getItem(position);
         final boolean isSelected = orderItem == _selected;
         LayoutInflater vi = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         final View card = Preconditions.checkNotNull(vi.inflate(R.layout.lt_sell_order_card, null));

         String price = String.format(_locale, "%s %s", orderItem.oneBtcInFiat, orderItem.currency);
         TextView tvPrice = (TextView) card.findViewById(R.id.tvPrice);
         tvPrice.setText(price);
         setPriceColor(tvPrice, orderItem);
         TextView tvDistance = (TextView) card.findViewById(R.id.tvDistance);
         tvDistance.setText(getDistanceString(orderItem));
         setDistanceColor(tvDistance, orderItem);
         String limits = String.format(_locale, "%d - %d %s", orderItem.minimumFiat, orderItem.maximumFiat,
               orderItem.currency);
         ((TextView) card.findViewById(R.id.tvLimits)).setText(limits);
         String trader = String.format(_locale, "%s", orderItem.nickname);
         final TextView tvTraderName = (TextView) card.findViewById(R.id.tvTrader);
         tvTraderName.setText(trader);
         if (orderItem.nickname.equals(_ltManager.getNickname())) {
            // it is our order, so lets show a different background
            setBackgroundResource(card.findViewById(R.id.thingWithBackground),
                  R.drawable.background_sell_order_card_own);
            tvTraderName.append(" " + _context.getString(R.string.lt_thats_you));
         }

         int traderAgeDays = (int) (orderItem.traderAgeMs / MS_PER_DAY);
         String traderAge = getTraderAgeString(traderAgeDays);
         TextView tvTraderAge = (TextView) card.findViewById(R.id.tvTraderAge);
         tvTraderAge.setText(traderAge);
         setTraderAgeColor(tvTraderAge, traderAgeDays);
         RatingBar ratingBar = (RatingBar) card.findViewById(R.id.seller_rating);
         float rating = LtAndroidUtils.calculate5StarRating(orderItem.successfulSales, orderItem.abortedSales,
               orderItem.successfulBuys, orderItem.abortedBuys, orderItem.traderAgeMs);
         ratingBar.setRating(rating);

         // Selected item displays more stuff
         if (isSelected) {
            card.findViewById(R.id.lvSelectedInfo).setVisibility(View.VISIBLE);

            // Expected Trade Time
            if (orderItem.tradeMedianMs == null) {
               card.findViewById(R.id.tvExpectedTimeLabel).setVisibility(View.GONE);
               card.findViewById(R.id.tvExpectedTime).setVisibility(View.GONE);
            } else {
               card.findViewById(R.id.tvExpectedTimeLabel).setVisibility(View.VISIBLE);
               TextView tvExpectedTime = (TextView) card.findViewById(R.id.tvExpectedTime);
               tvExpectedTime.setVisibility(View.VISIBLE);
               String hourString = LtAndroidUtils.getApproximateTimeInHours(_context, orderItem.tradeMedianMs);
               tvExpectedTime.setText(hourString);
            }
            // Location
            TextView tvLocation = (TextView) card.findViewById(R.id.tvLocation);
            tvLocation.setText(orderItem.location.name);

            // Description
            TextView tvDescriptionLabel = (TextView) card.findViewById(R.id.tvDescriptionLabel);
            TextView tvDescription = (TextView) card.findViewById(R.id.tvDescription);
            if (orderItem.description.trim().length() == 0) {
               tvDescription.setVisibility(View.GONE);
               tvDescriptionLabel.setVisibility(View.GONE);
            } else {
               tvDescription.setVisibility(View.VISIBLE);
               tvDescriptionLabel.setVisibility(View.VISIBLE);
               tvDescription.setText(orderItem.description);
            }

            // Button
            Button btBuy = (Button) card.findViewById(R.id.btBuy);
            if (orderItem.nickname.equals(_ltManager.getNickname())) {
               // This is ourselves, show edit button
               btBuy.setText(R.string.lt_edit_button);
               btBuy.setOnClickListener(editClickListener);
            } else {
               // Show buy button
               btBuy.setText(R.string.lt_buy_button);
               btBuy.setOnClickListener(buyClickListener);
            }

         } else {
            card.findViewById(R.id.lvSelectedInfo).setVisibility(View.GONE);
         }

         card.setOnClickListener(itemClickListener);
         card.setTag(new Tag(orderItem, position));
         return card;
      }

      private OnClickListener itemClickListener = new OnClickListener() {

         @Override
         public void onClick(View v) {
            Tag tag = (Tag) v.getTag();
            _selected = tag.item;
            ListView listView = (ListView) findViewById(R.id.lvRecords);
            _recordsAdapter.notifyDataSetChanged();
            listView.smoothScrollToPosition(tag.position);
         }
      };

      private String getTraderAgeString(int days) {
         return getResources().getString(R.string.lt_time_in_days, days);
      }

      private String getDistanceString(SellOrderSearchItem item) {
         if (isOmnipresent(item)) {
            // This trader is not limited by distance, and can trade anywhere in
            // the world
            // XXX use strings.xml
            return "Omnipresent";
         } else if (_useKilometers) {
            // XXX use strings.xml
            return String.format(_locale, "%d Km", item.distanceInMeters / 1000);
         } else {
            // XXX use strings.xml
            return String.format(_locale, "%d Miles", item.distanceInMeters / METERS_PR_MILE);
         }

      }
   }

   OnClickListener buyClickListener = new OnClickListener() {

      @Override
      public void onClick(View v) {
         CreateInstantBuyOrderActivity.callMe(getActivity(), _selected);
      }
   };

   OnClickListener editClickListener = new OnClickListener() {

      @Override
      public void onClick(View v) {
         SendRequestActivity.callMe(getActivity(), new GetSellOrder(_selected.id),
               getString(R.string.lt_edit_sell_order_title));
      }
   };

   private void setBackgroundResource(View theView, int backgroundResource) {
      int bottom = theView.getPaddingBottom();
      int top = theView.getPaddingTop();
      int right = theView.getPaddingRight();
      int left = theView.getPaddingLeft();
      theView.setBackgroundResource(backgroundResource);
      theView.setPadding(left, top, right, bottom);
   }

   private boolean isOmnipresent(SellOrderSearchItem item) {
      return item.location.latitude == 0 && item.location.longitude == 0;
   }

   private void setTraderAgeColor(TextView textView, int traderAgeDays) {
      if (traderAgeDays < 2) {
         setCol(textView, R.color.status_red);
      } else if (traderAgeDays < 30) {
         setCol(textView, R.color.status_yellow);
      } else {
         setCol(textView, R.color.status_green);
      }
   }

   private void setCol(TextView textView, int col) {
      textView.setTextColor(getResources().getColor(col));
   }

   private void setPriceColor(TextView textView, SellOrderSearchItem orderItem) {
      // For now the price color is always green
      setCol(textView, R.color.status_green);
      return;
   }

   private void setDistanceColor(TextView textView, SellOrderSearchItem orderItem) {
      if (isOmnipresent(orderItem)) {
         setCol(textView, R.color.status_green);
      } else if (orderItem.distanceInMeters > 100000) {
         setCol(textView, R.color.status_red);
      } else if (orderItem.distanceInMeters > 10000) {
         setCol(textView, R.color.status_yellow);
      } else {
         setCol(textView, R.color.status_green);
      }
   }

   private LocalTraderEventSubscriber ltSubscriber = new LocalTraderEventSubscriber(new Handler()) {

      @Override
      public void onLtError(int errorCode) {
         // handled by parent activity
      };

      @Override
      public void onLtSellOrderSearch(java.util.List<SellOrderSearchItem> result, SellOrderSearch request) {
         // Success
         if (isAdded()) {
            _sellOrders = result;
            updateUi(false);
         }
      }

   };
}