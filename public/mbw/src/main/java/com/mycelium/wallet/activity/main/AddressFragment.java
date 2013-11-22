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

package com.mycelium.wallet.activity.main;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.RecordManager;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity;
import com.mycelium.wallet.activity.util.QrImageView;
import com.mycelium.wallet.event.AddressBookChanged;
import com.mycelium.wallet.event.RecordSetChanged;
import com.mycelium.wallet.event.SelectedRecordChanged;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

public class AddressFragment extends Fragment {

   private View _root;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      _root = Preconditions.checkNotNull(inflater.inflate(R.layout.address_view, container, false));
      QrImageView qrButton = (QrImageView) Preconditions.checkNotNull(_root.findViewById(R.id.ivQR));
      qrButton.setQrCode(getUri());
      qrButton.setTapToCycleBrightness(false);
      qrButton.setOnClickListener(new QrClickListener());
      return _root;
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      setHasOptionsMenu(true);
      super.onCreate(savedInstanceState);
   }

   @Override
   public void onAttach(Activity activity) {
      super.onAttach(activity);
   }

   @Override
   public void onResume() {
      getEventBus().register(this);
      updateUi();
      super.onResume();
   }

   @Override
   public void onPause() {
      getEventBus().unregister(this);
      super.onPause();
   }

   @Override
   public void onDetach() {
      super.onDetach();
   }

   private MbwManager getMbwManager() {
      return MbwManager.getInstance(getActivity());
   }

   private Bus getEventBus() {
      return getMbwManager().getEventBus();
   }

   Record getRecord() {
      return getRecordManager().getSelectedRecord();
   }

   private RecordManager getRecordManager() {
      return getMbwManager().getRecordManager();
   }

   private void updateUi() {
      if (!isAdded()) {
         return;
      }

      // Update QR code
      QrImageView qrButton = (QrImageView) Preconditions.checkNotNull(_root.findViewById(R.id.ivQR));
      qrButton.setQrCode(getUri());

      // Update address
      Address address = getRecord().address;
      // Show name of bitcoin address according to address book
      TextView tvAddressTitle = (TextView) _root.findViewById(R.id.tvAddressLabel);
      String name = getMbwManager().getAddressBookManager().getNameByAddress(address.toString());
      if (name.length() == 0) {
         tvAddressTitle.setVisibility(View.GONE);
      } else {
         tvAddressTitle.setVisibility(View.VISIBLE);
         tvAddressTitle.setText(name);
      }

      // Set address
      String[] addressStrings = Utils.stringChopper(address.toString(), 12);
      ((TextView) _root.findViewById(R.id.tvAddress1)).setText(addressStrings[0]);
      ((TextView) _root.findViewById(R.id.tvAddress2)).setText(addressStrings[1]);
      ((TextView) _root.findViewById(R.id.tvAddress3)).setText(addressStrings[2]);
   }

   private String getUri() {
      return "bitcoin:" + getRecord().address.toString();
   }

   private class QrClickListener implements OnClickListener {
      @Override
      public void onClick(View v) {
         ReceiveCoinsActivity.callMe(AddressFragment.this.getActivity(), getRecord());
      }
   }

   /**
    * Fires when record set changed
    */
   @Subscribe
   public void recordSetChanged(RecordSetChanged event) {
      updateUi();
   }

   /**
    * Fires when the selected record changes
    */
   @Subscribe
   public void selectedRecordChanged(SelectedRecordChanged event) {
      updateUi();
   }

   /**
    * Fires when the address book changed
    */
   @Subscribe
   public void addressBookChanged(AddressBookChanged event) {
      updateUi();
   }
}
