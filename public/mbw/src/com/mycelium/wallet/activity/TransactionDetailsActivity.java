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

package com.mycelium.wallet.activity;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Joiner;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.mbwapi.api.ApiException;
import com.mrd.mbwapi.api.ApiObject;
import com.mrd.mbwapi.api.TransactionSummary;
import com.mrd.mbwapi.api.TransactionSummary.Item;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;

public class TransactionDetailsActivity extends Activity {

   private static final String BLOCKCHAIN_INFO_TRANSACTION_LINK_TEMPLATE = "https://blockchain.info/tx/";
   private static final String BLOCKCHAIN_INFO_ADDRESS_LINK_TEMPLATE = "https://blockchain.info/address/";
   private static final LayoutParams FPWC = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1);
   private static final LayoutParams WCWC = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1);
   private TransactionSummary _tx;
   private int _white_color;
   private UrlClickListener _urlClickListener;
   private MbwManager _mbwManager;

   /**
    * Called when the activity is first created.
    */
   @SuppressLint("ShowToast")
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      _white_color = getResources().getColor(R.color.white);
      _urlClickListener = new UrlClickListener();
      setContentView(R.layout.transaction_details_activity);
      _mbwManager = MbwManager.getInstance(this.getApplication());

      // Get intent parameters
      byte[] bytes = getIntent().getByteArrayExtra("transaction");
      if (bytes != null) {
         try {
            _tx = ApiObject.deserialize(TransactionSummary.class, new ByteReader(bytes));
         } catch (ApiException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
         }
      } else {
         finish();
         return;
      }
      updateUi();
   }

   private void setLinkText(TextView tv, String text) {
      SpannableString link = new SpannableString(text);
      link.setSpan(new UnderlineSpan(), 0, text.length(), 0);
      tv.setText(link);
      tv.setTextColor(getResources().getColor(R.color.brightblue));
   }

   private void updateUi() {
      // Set Hash
      String hash = _tx.hash.toString();
      String choppedHash = Joiner.on(" ").join(Utils.stringChopper(hash, 4));
      TextView tvHash = ((TextView) findViewById(R.id.tvHash));
      setLinkText(tvHash, choppedHash);
      tvHash.setTag(BLOCKCHAIN_INFO_TRANSACTION_LINK_TEMPLATE + hash);
      tvHash.setOnClickListener(_urlClickListener);

      // Set Confirmed
      String confirmed;
      if (_tx.height > 0) {
         confirmed = getResources().getString(R.string.confirmed_in_block, _tx.height);
      } else {
         confirmed = getResources().getString(R.string.no);
      }
      ((TextView) findViewById(R.id.tvConfirmed)).setText(confirmed);

      // Set Date & Time
      Date date = new Date(_tx.time * 1000L);
      Locale locale = getResources().getConfiguration().locale;
      DateFormat dayFormat = DateFormat.getDateInstance(DateFormat.LONG, locale);
      String dateString = dayFormat.format(date);
      ((TextView) findViewById(R.id.tvDate)).setText(dateString);
      DateFormat hourFormat = DateFormat.getTimeInstance(DateFormat.LONG, locale);
      String timeString = hourFormat.format(date);
      ((TextView) findViewById(R.id.tvTime)).setText(timeString);

      // Set Inputs
      LinearLayout inputs = (LinearLayout) findViewById(R.id.llInputs);
      for (Item item : _tx.inputs) {
         inputs.addView(getItemView(item));
      }

      // Set Outputs
      LinearLayout outputs = (LinearLayout) findViewById(R.id.llOutputs);
      for (Item item : _tx.outputs) {
         outputs.addView(getItemView(item));
      }

      // Set Fee
      String fee = _mbwManager.getBtcValueString(getFee(_tx));
      ((TextView) findViewById(R.id.tvFee)).setText(fee);

   }

   private long getFee(TransactionSummary tx) {
      long inputs = sum(tx.inputs);
      long outputs = sum(tx.outputs);
      return inputs - outputs;
   }

   private long sum(Item[] items) {
      long sum = 0;
      for (Item item : items) {
         sum += item.value;
      }
      return sum;
   }

   private View getItemView(Item item) {
      // Create vertical linear layout
      LinearLayout ll = new LinearLayout(this);
      ll.setOrientation(LinearLayout.VERTICAL);
      ll.setLayoutParams(WCWC);

      String address = item.address.toString();

      // Add BTC value
      ll.addView(getValue(item.value, address));

      // Add address chunks
      String[] chunks = Utils.stringChopper(address, 12);
      ll.addView(getAddressChunk(chunks[0], -1, address));
      ll.addView(getAddressChunk(chunks[1], -1, address));
      ll.addView(getAddressChunk(chunks[2], 0, address));

      ll.setPadding(10, 10, 10, 10);
      ll.setOnClickListener(_urlClickListener);
      ll.setTag(BLOCKCHAIN_INFO_ADDRESS_LINK_TEMPLATE + address);
      return ll;
   }

   private TextView getAddressChunk(String chunk, int padding, Object tag) {
      TextView tv = new TextView(this);
      tv.setLayoutParams(FPWC);
      tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
      setLinkText(tv, chunk);
      tv.setTypeface(Typeface.MONOSPACE);
      tv.setPadding(0, 0, 0, padding);
      tv.setTag(tag);

      return tv;
   }

   private View getValue(long value, Object tag) {
      TextView tv = new TextView(this);
      tv.setLayoutParams(FPWC);
      tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
      tv.setText(_mbwManager.getBtcValueString(value));
      tv.setTextColor(_white_color);
      tv.setTag(tag);
      return tv;
   }

   @SuppressWarnings("unused")
   private class CopyClickListener implements OnClickListener {

      @Override
      public void onClick(View v) {
         if (v.getTag() == null) {
            return;
         }
         Context context = TransactionDetailsActivity.this;
         ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
         clipboard.setText(v.getTag().toString());
         Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
      }
   }

   private class UrlClickListener implements OnClickListener {

      @Override
      public void onClick(View v) {
         if (v.getTag() == null) {
            return;
         }
         try {
            String url = v.getTag().toString();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
            Toast.makeText(TransactionDetailsActivity.this, R.string.redirecting_to_blockchain_info, Toast.LENGTH_SHORT)
                  .show();
         } catch (Exception e) {
            // Ignore
         }
      }
   }

}