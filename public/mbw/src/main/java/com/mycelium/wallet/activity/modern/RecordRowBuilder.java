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

package com.mycelium.wallet.activity.modern;

import java.util.Set;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wallet.BalanceInfo;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.Record.Tag;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.Wallet;

public class RecordRowBuilder {

   public static View buildRecordView(Resources resources, MbwManager mbwManager, LayoutInflater inflater,
         AddressBookManager addressBook, ViewGroup parent, Record record, boolean isSelected, boolean hasFocus,
         Set<Address> selectedAddresses) {
      View rowView = inflater.inflate(R.layout.record_row, parent, false);

      // Make grey if not part of the balance

      if (!selectedAddresses.contains(record.address)) {
         Utils.setAlpha(rowView, 0.5f);
      }

      int textColor = resources.getColor(R.color.white);

      // Show focus if applicable
      if (hasFocus) {
         rowView.setBackgroundColor(resources.getColor(R.color.selectedrecord));
      }

      // Show/hide key icon
      if (!record.hasPrivateKey()) {
         rowView.findViewById(R.id.ivKey).setVisibility(View.INVISIBLE);
      }

      // Set Label
      String address = record.address.toString();
      String name = addressBook.getNameByAddress(address);
      if (name.length() == 0) {
         ((TextView) rowView.findViewById(R.id.tvLabel)).setVisibility(View.GONE);
      } else {
         // Display name
         TextView tvLabel = ((TextView) rowView.findViewById(R.id.tvLabel));
         tvLabel.setVisibility(View.VISIBLE);
         tvLabel.setText(name);
         tvLabel.setTextColor(textColor);
      }

      String displayAddress;
      if (name.length() == 0) {
         // Display address in it's full glory, chopping it into three
         displayAddress = record.address.toMultiLineString();
      } else {
         // Display address in short form
         displayAddress = getShortAddress(address);
      }
      TextView tvAddress = ((TextView) rowView.findViewById(R.id.tvAddress));
      tvAddress.setText(displayAddress);
      tvAddress.setTextColor(textColor);

      // Set tag
      rowView.setTag(record);

      // Show selected address with a different color
      if (isSelected) {
         // rowView.setBackgroundColor(resources.getColor(R.color.black));
         // rowView.setBackgroundDrawable(resources.getDrawable(R.drawable.btn_blue_slim));
      } else {
         // rowView.setBackgroundDrawable(resources.getDrawable(R.drawable.btn_pitch_black_slim));
      }

      // Set balance
      if (record.tag == Tag.ACTIVE) {
         BalanceInfo balance = new Wallet(record).getLocalBalance(mbwManager.getBlockChainAddressTracker());
         if (balance.isKnown()) {
            rowView.findViewById(R.id.tvBalance).setVisibility(View.VISIBLE);
            String balanceString = mbwManager.getBtcValueString(balance.unspent + balance.pendingChange);
            TextView tvBalance = ((TextView) rowView.findViewById(R.id.tvBalance));
            tvBalance.setText(balanceString);
            tvBalance.setTextColor(textColor);
         } else {
            // We don't show anything if we don't know the balance
            rowView.findViewById(R.id.tvBalance).setVisibility(View.GONE);
         }
      } else {
         // We don't show anything if we don't know the address is archived
         rowView.findViewById(R.id.tvBalance).setVisibility(View.GONE);
      }

      // Show or hide backup verification warning
      if (record.needsBackupVerification()) {
         rowView.findViewById(R.id.tvNoBackupWarning).setVisibility(View.VISIBLE);
      } else {
         rowView.findViewById(R.id.tvNoBackupWarning).setVisibility(View.GONE);
      }

      // Show/hide key icon
      if (record.address.equals(mbwManager.getLocalTraderManager().getLocalTraderAddress())) {
         rowView.findViewById(R.id.tvTraderKey).setVisibility(View.VISIBLE);
      } else {
         rowView.findViewById(R.id.tvTraderKey).setVisibility(View.GONE);
      }

      return rowView;

   }

   private static String getShortAddress(String addressString) {
      StringBuilder sb = new StringBuilder();
      sb.append(addressString.substring(0, 6));
      sb.append("...");
      sb.append(addressString.substring(addressString.length() - 6));
      return sb.toString();
   }

}
