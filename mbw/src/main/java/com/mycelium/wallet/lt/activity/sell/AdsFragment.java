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

package com.mycelium.wallet.lt.activity.sell;

import java.text.DateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.mycelium.lt.api.model.Ad;
import com.mycelium.lt.api.model.AdType;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.api.ActivateAd;
import com.mycelium.wallet.lt.api.DeactivateAd;
import com.mycelium.wallet.lt.api.DeleteAd;
import com.mycelium.wallet.lt.api.GetAds;

import static com.google.common.base.Preconditions.checkNotNull;

public class AdsFragment extends Fragment {

   private MbwManager _mbwManager;
   private LocalTraderManager _ltManager;
   private ActionMode _currentActionMode;
   private List<Ad> _ads;
   private Ad _selectedAd;
   private ListView _lvAds;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View ret = checkNotNull(inflater.inflate(R.layout.lt_ads_fragment, container, false));
      setHasOptionsMenu(true);
      _lvAds = (ListView) ret.findViewById(R.id.lvAds);
      _lvAds.setOnItemClickListener(itemListClickListener);
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
      _ltManager.subscribe(ltSubscriber);
      if (_ltManager.hasLocalTraderAccount()) {
         updateUi();
         _ltManager.makeRequest(new GetAds());
      } else {
         _ads = new LinkedList<>();
         updateUi();
      }
      super.onResume();
   }

   @Override
   public void onPause() {
      _ltManager.unsubscribe(ltSubscriber);
      super.onPause();
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      if (item.getItemId() == R.id.miAddAd) {
         CreateOrEditAdActivity.callMe(this.getActivity());
      }
      return super.onOptionsItemSelected(item);
   }

   private void setAds(Collection<Ad> ads) {
      _ads = new LinkedList<>(ads);
      Collections.sort(_ads, new Comparator<Ad>() {

         @Override
         public int compare(Ad lhs, Ad rhs) {
            // First sort on ad type
            if (lhs.type.ordinal() < rhs.type.ordinal()) {
               return -1;
            } else if (lhs.type.ordinal() > rhs.type.ordinal()) {
               return 1;
            }
            // Then compare by price formula
            int c = lhs.priceFormula.id.compareTo(rhs.priceFormula.id);
            if (c != 0) {
               return c;
            }
            // Then sort on premium
            if (lhs.premium < rhs.premium) {
               return -1;
            } else if (lhs.premium > rhs.premium) {
               return 1;
            }
            // Finally sort on id
            return lhs.id.compareTo(rhs.id);
         }
      });
      updateUi();
   }

   private void updateUi() {
      if (!isAdded()) {
         return;
      }
      if (_ads == null) {
         findViewById(R.id.pbWait).setVisibility(View.VISIBLE);
         findViewById(R.id.tvNoRecords).setVisibility(View.GONE);
         _lvAds.setVisibility(View.GONE);
      } else if (_ads.size() == 0) {
         findViewById(R.id.pbWait).setVisibility(View.GONE);
         findViewById(R.id.tvNoRecords).setVisibility(View.VISIBLE);
         _lvAds.setVisibility(View.GONE);
      } else {
         findViewById(R.id.pbWait).setVisibility(View.GONE);
         findViewById(R.id.tvNoRecords).setVisibility(View.GONE);
         _lvAds.setVisibility(View.VISIBLE);
         _lvAds.setAdapter(new AdsAdapter(getActivity(), _ads));
      }
   }

   @Override
   public void setUserVisibleHint(boolean isVisibleToUser) {
      super.setUserVisibleHint(isVisibleToUser);
      if (!isVisibleToUser) {
         finishActionMode();
      }
   }

   private void finishActionMode() {
      if (_currentActionMode != null) {
         _currentActionMode.finish();
      }
   }

   OnItemClickListener itemListClickListener = new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> listView, final View view, final int position, long id) {
         _selectedAd = (Ad) view.getTag();
         AppCompatActivity parent = (AppCompatActivity) getActivity();
         _currentActionMode = parent.startSupportActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
               actionMode.getMenuInflater().inflate(R.menu.lt_ads_context_menu, menu);
               updateActionBar(actionMode, menu);
               return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
               updateActionBar(actionMode, menu);
               return true;
            }

            private void updateActionBar(ActionMode actionMode, Menu menu) {
               _currentActionMode = actionMode;
               menu.findItem(R.id.miDeactivate).setVisible(_selectedAd.isActive);
               menu.findItem(R.id.miActivate).setVisible(!_selectedAd.isActive);
               _lvAds.setItemChecked(position, true);
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
               final int item = menuItem.getItemId();
               if (item == R.id.miDelete) {
                  confirmDeleteEntry();
                  return true;
               } else if (item == R.id.miEdit) {
                  _mbwManager.runPinProtectedFunction(getActivity(), pinProtectedEditEntry);
                  return true;
               } else if (item == R.id.miActivate) {
                  _ltManager.makeRequest(new ActivateAd(_selectedAd.id));
                  finishActionMode();
                  return true;
               } else if (item == R.id.miDeactivate) {
                  _ltManager.makeRequest(new DeactivateAd(_selectedAd.id));
                  finishActionMode();
                  return true;
               }
               return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
               _lvAds.setItemChecked(position, false);
               _currentActionMode = null;
            }
         });
      }
   };

   final Runnable pinProtectedEditEntry = new Runnable() {

      @Override
      public void run() {
         doEditEntry();
      }
   };

   private void doEditEntry() {
      if (_selectedAd != null) {
         CreateOrEditAdActivity.callMe(getActivity(), _selectedAd);
      }
      finishActionMode();
   }

   private void confirmDeleteEntry() {
      AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this.getActivity());

      // Set title
      alertDialogBuilder.setTitle(R.string.lt_confirm_delete_sell_order_title);
      // Set dialog message
      alertDialogBuilder.setMessage(R.string.lt_confirm_delete_sell_order_message);
      // Yes action
      alertDialogBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int id) {
            dialog.dismiss();
            _mbwManager.runPinProtectedFunction(getActivity(), pinProtectedDeleteEntry);
            finishActionMode();
         }
      });
      // No action
      alertDialogBuilder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int id) {
            dialog.cancel();
         }
      });
      alertDialogBuilder.create().show();

   }

   final Runnable pinProtectedDeleteEntry = new Runnable() {

      @Override
      public void run() {
         doDeleteEntry();
      }
   };

   private void doDeleteEntry() {
      if (_selectedAd == null) {
         return;
      }
      _ltManager.makeRequest(new DeleteAd(_selectedAd.id));
   }

   private class AdsAdapter extends ArrayAdapter<Ad> {
      private Locale _locale;
      private Context _context;
      private DateFormat _dateFormat;

      AdsAdapter(Context context, List<Ad> objects) {
         super(context, R.layout.lt_ad_row, objects);
         _locale = new Locale("en", "US");

         _dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, getResources().getConfiguration().locale);
         _context = context;
      }

      @Override
      @NonNull
      public View getView(int position, View convertView, @NonNull ViewGroup parent) {
         View v = convertView;

         if (v == null) {
            LayoutInflater vi = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = checkNotNull(vi.inflate(R.layout.lt_ad_row, parent, false));
         }
         Ad o = checkNotNull(getItem(position));
         char sign = o.premium >= 0 ? '+' : '-';
         double d = Math.abs(o.premium);
         String premium = d == (int) d ? String.format(_locale, "%d", (int) d) : String.format(_locale, "%s", d);
         String description1 = getString(o.type == AdType.SELL_BTC ? R.string.lt_selling_near : R.string.lt_buying_near);
         String description2 = o.location.name;
         String description3 = String.format(_locale, "%s%s %c%s%%",
                 o.priceFormula.name,
                 o.priceFormula.available ? "" : " (" + getString(R.string.lt_price_source_not_available) + ")",
                 sign,
                 premium);

         String description4 = String.format(_locale, "%d %s - %s %s", o.minimumFiat, o.currency, o.maximumFiat,
               o.currency);
         ((TextView) v.findViewById(R.id.tvDescription1)).setText(description1);
         ((TextView) v.findViewById(R.id.tvDescription2)).setText(description2);
         ((TextView) v.findViewById(R.id.tvDescription3)).setText(description3);
         ((TextView) v.findViewById(R.id.tvDescription4)).setText(description4);
         TextView tvActive = (TextView) v.findViewById(R.id.tvActive);
         if (o.isActive) {
            tvActive.setTextColor(getActivity().getResources().getColor(R.color.green));
            String dateString = _dateFormat.format(new Date(o.deactivationTime));
            tvActive.setText(getActivity().getResources().getString(R.string.lt_order_active, dateString));
         } else {
            tvActive.setTextColor(getActivity().getResources().getColor(R.color.red));
            tvActive.setText(getActivity().getResources().getString(R.string.lt_order_inactive));
         }
         v.setTag(o);
         return v;
      }
   }

   private LocalTraderEventSubscriber ltSubscriber = new LocalTraderEventSubscriber(new Handler()) {
      @Override
      public void onLtAdsFetched(java.util.Collection<Ad> ads, GetAds request) {
         setAds(ads);
      }

      @Override
      public void onLtAdDeleted(UUID adId, DeleteAd request) {
         _ltManager.makeRequest(new GetAds());
      }

      @Override
      public void onLtAdActivated(UUID adId, ActivateAd request) {
         _ltManager.makeRequest(new GetAds());
      }

      @Override
      public void onLtAdDeactivated(UUID adId, DeactivateAd request) {
         _ltManager.makeRequest(new GetAds());
      }

      @Override
      public void onLtError(int errorCode) {
         // handled by parent activity
      }
   };
}
