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

package com.mycelium.wallet.activity.send;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.BitcoinUri;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.Utils.BitcoinScanResult;
import com.mycelium.wallet.activity.addressbook.AddressChooserActivity;

public class GetReceivingAddressActivity extends Activity {

   public static final int SCANNER_RESULT_CODE = 0;
   private static final int ADDRESS_BOOK_RESULT_CODE = 1;
   private static final int MANUAL_ENTRY_RESULT_CODE = 2;
   private MbwManager _mbwManager;

   public static void callMe(Activity currentActivity, int requestCode) {
      Intent intent = new Intent(currentActivity, GetReceivingAddressActivity.class);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.get_receiving_address_activity);

      _mbwManager = MbwManager.getInstance(getApplication());

      final Address clipboardAddress = getClipboardAddress();
      if (clipboardAddress == null) {
         findViewById(R.id.btClipboard).setEnabled(false);
      } else {
         findViewById(R.id.btClipboard).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
               // Return clipboard addressas result
               Intent result = new Intent();
               result.putExtra("receivingAddress", clipboardAddress);
               setResult(RESULT_OK, result);
               GetReceivingAddressActivity.this.finish();
            }
         });
      }

      findViewById(R.id.btScan).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            Utils.startScannerIntent(GetReceivingAddressActivity.this, SCANNER_RESULT_CODE,
                  _mbwManager.getContinuousFocus());
         }
      });

      findViewById(R.id.btAddressBook).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            Intent intent = new Intent(GetReceivingAddressActivity.this, AddressChooserActivity.class);
            startActivityForResult(intent, ADDRESS_BOOK_RESULT_CODE);
         }
      });

      findViewById(R.id.btAddressBook).setEnabled(_mbwManager.getAddressBookManager().numEntries() != 0);

      findViewById(R.id.btManual).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View view) {
            Intent intent = new Intent(GetReceivingAddressActivity.this, ManualAddressEntry.class);
            startActivityForResult(intent, MANUAL_ENTRY_RESULT_CODE);
         }
      });

   }

   private Address getClipboardAddress() {
      ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
      CharSequence content = clipboard.getText();
      if (content == null) {
         return null;
      }
      String string = content.toString();
      String addressString;
      if (string.matches("[a-zA-Z0-9]*")) {
         // Raw format
         addressString = string;
      } else {
         BitcoinUri b = BitcoinUri.parse(string);
         if (b == null) {
            // Not on URI format
            return null;
         } else {
            // On URI format
            addressString = b.getAddress().trim();
         }
      }

      // Is it really an address?
      Address address = Address.fromString(addressString, Constants.network);
      return address;
   }

   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == ADDRESS_BOOK_RESULT_CODE && resultCode == RESULT_OK) {
         // Get result from address chooser
         String s = Preconditions.checkNotNull(intent.getStringExtra(AddressChooserActivity.ADDRESS_RESULT_NAME));
         String result = s.trim();
         // Is it really an address?
         Address address = Address.fromString(result, Constants.network);
         if (address != null) {
            // Return address as result
            Intent intentResult = new Intent();
            intentResult.putExtra("receivingAddress", address);
            setResult(RESULT_OK, intentResult);
            GetReceivingAddressActivity.this.finish();
         }
      } else if (requestCode == SCANNER_RESULT_CODE && resultCode == RESULT_OK) {
         BitcoinScanResult scanResult = Utils.parseScanResult(intent);

         // Bail out if we don't like the result
         if (scanResult == null || scanResult.address == null) {
            Toast.makeText(this, R.string.invalid_bitcoin_uri, Toast.LENGTH_LONG).show();
            return;
         }

         // Return result in an intent
         Intent intentResult = new Intent();
         intentResult.putExtra("receivingAddress", scanResult.address);
         intentResult.putExtra("amountToSend", scanResult.amount); // May be
                                                                   // null
         setResult(RESULT_OK, intentResult);
         GetReceivingAddressActivity.this.finish();
      } else if (requestCode == MANUAL_ENTRY_RESULT_CODE && resultCode == RESULT_OK) {
         Address address = Preconditions.checkNotNull((Address) intent
               .getSerializableExtra(ManualAddressEntry.ADDRESS_RESULT_NAME));

         // Return result in an intent
         Intent intentResult = new Intent();
         intentResult.putExtra("receivingAddress", address);
         setResult(RESULT_OK, intentResult);
         GetReceivingAddressActivity.this.finish();
      }
   }
}