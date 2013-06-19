/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 *  Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 *  This license governs use of the accompanying software. If you use the software, you accept this license.
 *  If you do not accept the license, do not use the software.
 *
 *  1. Definitions
 *  The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 *  "You" means the licensee of the software.
 *  "Your company" means the company you worked for when you downloaded the software.
 *  "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 *  of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 *  software, and specifically excludes the right to distribute the software outside of your company.
 *  "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 *  under this license.
 *
 *  2. Grant of Rights
 *  (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free copyright license to reproduce the software for reference use.
 *  (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free patent license under licensed patents for reference use.
 *
 *  3. Limitations
 *  (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 *  (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 *  (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 *  (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 *  guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 *  change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 *  fitness for a particular purpose and non-infringement.
 *
 */

package com.mycelium.wallet.activity.addressbook;

import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mrd.bitlib.util.StringUtils;
import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wallet.AddressBookManager.Entry;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;

public class AddressBookActivity extends ListActivity {

   private String mSelectedAddress;
   private MbwManager _mbwManager;
   private AddressBookManager _addressBook;
   private AlertDialog _qrCodeDialog;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.address_book);
      registerForContextMenu(getListView());
      _mbwManager = MbwManager.getInstance(getApplication());
      _addressBook = _mbwManager.getAddressBookManager();
   }

   @Override
   public void onResume() {
      super.onResume();
      updateEntries();
   }

   @Override
   protected void onDestroy() {
      if (_qrCodeDialog != null) {
         _qrCodeDialog.dismiss();
      }
      super.onDestroy();
   }

   public void updateEntries() {
      List<Entry> entries = _addressBook.getEntries();
      if (entries.isEmpty()) {
         Toast.makeText(this, R.string.address_book_empty, Toast.LENGTH_SHORT).show();
      }
      setListAdapter(new AddressBookAdapter(this, R.layout.address_book_row, entries));
   }

   @Override
   protected void onListItemClick(ListView l, View v, int position, long id) {
      mSelectedAddress = (String) v.getTag();
      l.showContextMenuForChild(v);
   };

   @Override
   public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.addressbook_context_menu, menu);
   }

   @Override
   public boolean onContextItemSelected(final MenuItem item) {
      if (item.getItemId() == R.id.miDeleteAddress) {
         doDeleteEntry();
         return true;
      } else if (item.getItemId() == R.id.miEditAddress) {
         doEditEntry();
         return true;
      } else if (item.getItemId() == R.id.miShowQrCode) {
         doShowQrCode();
         return true;
      } else {
         return false;
      }
   }

   private void doEditEntry() {
      Utils.showSetAddressLabelDialog(this, _addressBook, mSelectedAddress, new Runnable() {

         @Override
         public void run() {
            updateEntries();
         }
      });
   }

   private void doShowQrCode() {
      String address = "bitcoin:" + mSelectedAddress;
      Bitmap bitmap = Utils.getLargeQRCodeBitmap(address, _mbwManager);
      _qrCodeDialog = Utils.showQrCode(this, R.string.bitcoin_address_title, bitmap, mSelectedAddress,
            R.string.copy_address_to_clipboard);
   }

   private void doDeleteEntry() {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setMessage(R.string.delete_address_confirmation).setCancelable(false)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
                  dialog.cancel();
                  _addressBook.deleteEntry(mSelectedAddress);
                  updateEntries();
               }
            }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
               }
            });
      AlertDialog alertDialog = builder.create();
      alertDialog.show();
   }

   private class AddressBookAdapter extends ArrayAdapter<Entry> {

      public AddressBookAdapter(Context context, int textViewResourceId, List<Entry> objects) {
         super(context, textViewResourceId, objects);
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
         View v = convertView;

         if (v == null) {
            LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.address_book_row, null);
         }
         TextView tvName = (TextView) v.findViewById(R.id.address_book_name);
         TextView tvAddress = (TextView) v.findViewById(R.id.address_book_address);
         Entry e = getItem(position);
         tvName.setText(e.getName());
         tvAddress.setText(formatAddress(e.getAddress()));
         v.setTag(e.getAddress());
         return v;
      }
   }

   private static String formatAddress(String address) {
      return StringUtils.join(Utils.stringChopper(address, 12), "\r\n");
   }

}
