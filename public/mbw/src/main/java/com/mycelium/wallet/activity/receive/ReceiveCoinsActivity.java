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

package com.mycelium.wallet.activity.receive;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.CoinUtil;
import com.mycelium.wallet.BuildConfig;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.GetAmountActivity;
import com.mycelium.wallet.activity.util.QrImageView;

//todo HD for the future: keep receiving slots for 20 addresses. assign a name

public class ReceiveCoinsActivity extends Activity {

   private static final int GET_AMOUNT_RESULT_CODE = 1;

   private MbwManager _mbwManager;
   private Address _address;
   private boolean _havePrivateKey;
   private Long _amount;

   public static void callMe(Activity currentActivity, Address address, boolean havePrivateKey) {
      Intent intent = new Intent(currentActivity, ReceiveCoinsActivity.class);
      intent.putExtra("address", address);
      intent.putExtra("havePrivateKey", havePrivateKey);
      currentActivity.startActivity(intent);
   }

   /**
    * Called when the activity is first created.
    */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.receive_coins_activity);

      _mbwManager = MbwManager.getInstance(getApplication());

      // Get intent parameters
      _address = Preconditions.checkNotNull((Address) getIntent().getSerializableExtra("address"));
      _havePrivateKey = getIntent().getBooleanExtra("havePrivateKey", false);

      // Load saved state
      if (savedInstanceState != null) {
         _amount = savedInstanceState.getLong("amount", -1);
         if (_amount == -1) {
            _amount = null;
         }
      }

      // Enter Amount
      findViewById(R.id.btEnterAmount).setOnClickListener(amountClickListener);

      // Amount Hint
      ((TextView) findViewById(R.id.tvAmount)).setHint(getResources().getString(R.string.amount_hint_denomination,
            _mbwManager.getBitcoinDenomination().toString()));

      shareByNfc();
   }

   @TargetApi(16)
   protected void shareByNfc() {
      if (Build.VERSION.SDK_INT < 16){
         // the function isNdefPushEnabled is only available for SdkVersion >= 16
         // We would be theoretically able to push the message over Ndef, but it is not
         // possible to check if Ndef/NFC is available or not - so dont try it at all, if
         // SdkVersion is too low
         return;
      }

      NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);
      if (nfc!=null && nfc.isNdefPushEnabled() ) {
         nfc.setNdefPushMessageCallback(new NfcAdapter.CreateNdefMessageCallback() {
            @Override
            public NdefMessage createNdefMessage(NfcEvent event) {
               NdefRecord uriRecord = NdefRecord.createUri(getPaymentUri());
               return new NdefMessage(new NdefRecord[]{uriRecord});
            }
         }, this);
         findViewById(R.id.ivNfc).setVisibility(View.VISIBLE);
      } else {
         findViewById(R.id.ivNfc).setVisibility(View.GONE);
      }
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      if (_amount != null) {
         outState.putLong("amount", _amount);
      }
      super.onSaveInstanceState(outState);
   }

   @Override
   protected void onResume() {
      updateUi();
      super.onResume();
   }

   private void updateUi() {
      final String qrText = getPaymentUri();

      if (_amount == null) {
         ((TextView) findViewById(R.id.tvTitle)).setText(R.string.bitcoin_address_title);
         ((Button) findViewById(R.id.btShare)).setText(R.string.share_bitcoin_address);
         ((TextView) findViewById(R.id.tvAmountLabel)).setText(R.string.optional_amount);
         ((TextView) findViewById(R.id.tvAmount)).setText("");
      } else {
         ((TextView) findViewById(R.id.tvTitle)).setText(R.string.payment_request);
         ((Button) findViewById(R.id.btShare)).setText(R.string.share_payment_request);
         ((TextView) findViewById(R.id.tvAmountLabel)).setText(R.string.amount_title);
         ((TextView) findViewById(R.id.tvAmount)).setText(_mbwManager.getBtcValueString(_amount));
      }

      // QR code
      ImageView imageView = (ImageView) findViewById(R.id.ivQrCode);
      //two-step cast to not confuse lint
      QrImageView iv = (QrImageView) imageView;
      iv.setQrCode(qrText);

      // Show warning if the record has no private key
      if (_havePrivateKey) {
         findViewById(R.id.tvWarning).setVisibility(View.GONE);
      } else {
         findViewById(R.id.tvWarning).setVisibility(View.VISIBLE);
      }

      String[] addressStrings = Utils.stringChopper(getBitcoinAddress(), 12);
      ((TextView) findViewById(R.id.tvAddress1)).setText(addressStrings[0]);
      ((TextView) findViewById(R.id.tvAddress2)).setText(addressStrings[1]);
      ((TextView) findViewById(R.id.tvAddress3)).setText(addressStrings[2]);

      updateAmount();
   }

   private void updateAmount() {
      if (_amount == null) {
         // No amount to show
         ((TextView) findViewById(R.id.tvAmount)).setText("");
      } else {
         // Set Amount
         ((TextView) findViewById(R.id.tvAmount)).setText(_mbwManager.getBtcValueString(_amount));
      }
   }

   private String getPaymentUri() {
      final StringBuilder uri = new StringBuilder("bitcoin:");
       uri.append(getBitcoinAddress());
      if (_amount != null) {
         uri.append("?amount=").append(CoinUtil.valueString(_amount, false));
      }
      return uri.toString();
   }

   private String getBitcoinAddress() {
      return _address.toString();
   }

   public void shareRequest(View view) {
       Intent s = new Intent(android.content.Intent.ACTION_SEND);
       s.setType("text/plain");
      if (_amount == null) {
         s.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.bitcoin_address_title));
         s.putExtra(Intent.EXTRA_TEXT, getBitcoinAddress());
         startActivity(Intent.createChooser(s, getResources().getString(R.string.share_bitcoin_address)));
      } else {
         s.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.payment_request));
         s.putExtra(Intent.EXTRA_TEXT, getPaymentUri());
         startActivity(Intent.createChooser(s, getResources().getString(R.string.share_payment_request)));
      }
   }

   public void copyToClipboard(View view) {
      String text;
      if (_amount == null) {
         text = getBitcoinAddress();
      } else {
         text = getPaymentUri();
      }
      Utils.setClipboardString(text, this);
      Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
   }

   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == GET_AMOUNT_RESULT_CODE && resultCode == RESULT_OK) {
         // Get result from address chooser (may be null)
         _amount = (Long) intent.getSerializableExtra("amount");
      } else {
         // We didn't like what we got, bail
      }
   }

   private OnClickListener amountClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         GetAmountActivity.callMe(ReceiveCoinsActivity.this, _amount, GET_AMOUNT_RESULT_CODE);
      }
   };

}