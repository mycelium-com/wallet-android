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
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
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

import com.google.common.base.Preconditions;
import com.mycelium.lt.api.model.SellOrder;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.api.ActivateSellOrder;
import com.mycelium.wallet.lt.api.DeactivateSellOrder;
import com.mycelium.wallet.lt.api.DeleteSellOrder;
import com.mycelium.wallet.lt.api.GetSellOrders;

public class SellOrdersFragment extends Fragment {

   private MbwManager _mbwManager;
   private LocalTraderManager _ltManager;
   private ActionMode _currentActionMode;
   private List<SellOrder> _sellOrders;
   private SellOrder _selectedSellOrder;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View ret = Preconditions.checkNotNull(inflater.inflate(R.layout.lt_sell_orders_fragment, container, false));
      setHasOptionsMenu(true);
      ListView ordersList = (ListView) ret.findViewById(R.id.lvSellOrders);
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
   public void onDetach() {
      super.onDetach();
   }

   @Override
   public void onResume() {
      _ltManager.subscribe(ltSubscriber);
      if (_ltManager.hasLocalTraderAccount()) {
         updateUi();
         _ltManager.makeRequest(new GetSellOrders());
      } else {
         _sellOrders = new LinkedList<SellOrder>();
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
   public void onDestroy() {
      super.onDestroy();
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      if (item.getItemId() == R.id.miAddSellOrder) {
         CreateOrEditSellOrderActivity.callMe(this.getActivity());
      }
      return super.onOptionsItemSelected(item);
   }

   private void setSellOrders(Collection<SellOrder> sellOrders) {
      _sellOrders = new LinkedList<SellOrder>(sellOrders);
      Collections.sort(_sellOrders, new Comparator<SellOrder>() {

         @Override
         public int compare(SellOrder lhs, SellOrder rhs) {
            // First compare by price formula
            int c = lhs.priceFormula.id.compareTo(rhs.priceFormula.id);
            if (c != 0) {
               return c;
            }
            // Second sort on premium
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
      if (_sellOrders == null) {
         findViewById(R.id.pbWait).setVisibility(View.VISIBLE);
         findViewById(R.id.tvNoRecords).setVisibility(View.GONE);
         findViewById(R.id.lvSellOrders).setVisibility(View.GONE);
      } else if (_sellOrders.size() == 0) {
         findViewById(R.id.pbWait).setVisibility(View.GONE);
         findViewById(R.id.tvNoRecords).setVisibility(View.VISIBLE);
         findViewById(R.id.lvSellOrders).setVisibility(View.GONE);
      } else {
         findViewById(R.id.pbWait).setVisibility(View.GONE);
         findViewById(R.id.tvNoRecords).setVisibility(View.GONE);
         findViewById(R.id.lvSellOrders).setVisibility(View.VISIBLE);
         ListView list = (ListView) findViewById(R.id.lvSellOrders);
         list.setAdapter(new SellOrderAdapter(getActivity(), _sellOrders));
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
      public void onItemClick(AdapterView<?> listView, final View view, int position, long id) {
         _selectedSellOrder = (SellOrder) view.getTag();
         ActionBarActivity parent = (ActionBarActivity) getActivity();
         _currentActionMode = parent.startSupportActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
               actionMode.getMenuInflater().inflate(R.menu.lt_sell_orders_context_menu, menu);
               return true;
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
               _currentActionMode = actionMode;
               view.setBackgroundDrawable(getResources().getDrawable(R.color.selectedrecord));
               menu.findItem(R.id.miDeactivate).setVisible(_selectedSellOrder.isActive);
               menu.findItem(R.id.miActivate).setVisible(!_selectedSellOrder.isActive);
               return true;
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
                  _ltManager.makeRequest(new ActivateSellOrder(_selectedSellOrder.id));
                  finishActionMode();
                  return true;
               } else if (item == R.id.miDeactivate) {
                  _ltManager.makeRequest(new DeactivateSellOrder(_selectedSellOrder.id));
                  finishActionMode();
                  return true;
               }
               return false;
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
               view.setBackgroundDrawable(null);
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
      if (_selectedSellOrder != null) {
         CreateOrEditSellOrderActivity.callMe(getActivity(), _selectedSellOrder);
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
      if (_selectedSellOrder == null) {
         return;
      }
      _ltManager.makeRequest(new DeleteSellOrder(_selectedSellOrder.id));
   }

   private class SellOrderAdapter extends ArrayAdapter<SellOrder> {
      private Locale _locale;
      private Context _context;
      private DateFormat _dateFormat;

      public SellOrderAdapter(Context context, List<SellOrder> objects) {
         super(context, R.layout.lt_sell_order_row, objects);
         _locale = new Locale("en", "US");

         _dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, getResources().getConfiguration().locale);
         _context = context;
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
         View v = convertView;

         if (v == null) {
            LayoutInflater vi = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = Preconditions.checkNotNull(vi.inflate(R.layout.lt_sell_order_row, null));
         }
         SellOrder o = getItem(position);
         char sign = o.premium > 0 ? '+' : '-';
         double d = Math.abs(o.premium);
         String premium = d == (int) d ? String.format(_locale, "%d", (int) d) : String.format(_locale, "%s", d);
         String description1 = o.location.name;
         String description2 = String.format(_locale, "%s %c%s%%", o.priceFormula.name, sign, premium);
         String description3 = String.format(_locale, "%d %s - %s %s", o.minimumFiat, o.currency, o.maximumFiat,
               o.currency);
         ((TextView) v.findViewById(R.id.tvDescription1)).setText(description1);
         ((TextView) v.findViewById(R.id.tvDescription2)).setText(description2);
         ((TextView) v.findViewById(R.id.tvDescription3)).setText(description3);
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
      public void onLtSellOrdersFetched(java.util.Collection<SellOrder> sellOrders, GetSellOrders request) {
         setSellOrders(sellOrders);
      };

      @Override
      public void onLtSellOrderDeleted(UUID sellOrderId, DeleteSellOrder request) {
         _ltManager.makeRequest(new GetSellOrders());
      }

      @Override
      public void onLtSellOrderActivated(UUID _sellOrderId, ActivateSellOrder request) {
         _ltManager.makeRequest(new GetSellOrders());
      };

      @Override
      public void onLtSellOrderDeactivated(UUID _sellOrderId, DeactivateSellOrder request) {
         _ltManager.makeRequest(new GetSellOrders());
      };

      @Override
      public void onLtError(int errorCode) {
         // handled by parent activity
      };

   };

}
