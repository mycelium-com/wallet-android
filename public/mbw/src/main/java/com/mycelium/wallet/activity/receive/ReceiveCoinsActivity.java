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
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.CoinUtil;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.GetAmountActivity;
import com.mycelium.wallet.activity.util.QrImageView;
import com.mycelium.wapi.wallet.currency.BitcoinValue;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExchangeBasedBitcoinValue;

//todo HD for the future: keep receiving slots for 20 addresses. assign a name

public class ReceiveCoinsActivity extends Activity {

   private static final int GET_AMOUNT_RESULT_CODE = 1;

   @InjectView(R.id.tvAmountLabel) TextView tvAmountLabel;
   @InjectView(R.id.tvAmount) TextView tvAmount;
   @InjectView(R.id.tvWarning) TextView tvWarning;
   @InjectView(R.id.tvTitle) TextView tvTitle;
   @InjectView(R.id.tvAddress1) TextView tvAddress1;
   @InjectView(R.id.tvAddress2) TextView tvAddress2;
   @InjectView(R.id.tvAddress3) TextView tvAddress3;
   @InjectView(R.id.ivNfc) ImageView ivNfc;
   @InjectView(R.id.ivQrCode) QrImageView ivQrCode;
   @InjectView(R.id.btShare) Button btShare;

   private MbwManager _mbwManager;
   private Address _address;
   private boolean _havePrivateKey;
   private CurrencyValue _amount;

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
      ButterKnife.inject(this);

      _mbwManager = MbwManager.getInstance(getApplication());

      // Get intent parameters
      _address = Preconditions.checkNotNull((Address) getIntent().getSerializableExtra("address"));
      _havePrivateKey = getIntent().getBooleanExtra("havePrivateKey", false);

      // Load saved state
      if (savedInstanceState != null) {
         _amount = (CurrencyValue) savedInstanceState.getSerializable("amount");
      }


      // Amount Hint
      tvAmount.setHint(getResources().getString(R.string.amount_hint_denomination,
            _mbwManager.getBitcoinDenomination().toString()));

      shareByNfc();
   }

   @TargetApi(16)
   protected void shareByNfc() {
      if (Build.VERSION.SDK_INT < 16) {
         // the function isNdefPushEnabled is only available for SdkVersion >= 16
         // We would be theoretically able to push the message over Ndef, but it is not
         // possible to check if Ndef/NFC is available or not - so dont try it at all, if
         // SdkVersion is too low
         return;
      }

      NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);
      if (nfc != null && nfc.isNdefPushEnabled()) {
         nfc.setNdefPushMessageCallback(new NfcAdapter.CreateNdefMessageCallback() {
            @Override
            public NdefMessage createNdefMessage(NfcEvent event) {
               NdefRecord uriRecord = NdefRecord.createUri(getPaymentUri());
               return new NdefMessage(new NdefRecord[]{uriRecord});
            }
         }, this);
         ivNfc.setVisibility(View.VISIBLE);
         ivNfc.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
               Utils.showSimpleMessageDialog(ReceiveCoinsActivity.this, getString(R.string.nfc_payment_request_hint));
            }
         });
      } else {
         ivNfc.setVisibility(View.GONE);
      }
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      if (_amount != null) {
         outState.putSerializable("amount", _amount);
      }
      super.onSaveInstanceState(outState);
   }

   @Override
   protected void onResume() {
      updateUi();
      super.onResume();
   }

   BitcoinValue getBitcoinAmount() {
      if (_amount == null) {
         return null;
      }

      if (!_amount.isBtc()) {
         // convert the amount to btc, but only once and stay within btc for all next calls
         _amount = ExchangeBasedBitcoinValue.fromValue(_amount, _mbwManager.getExchangeRateManager());
      }

      return (BitcoinValue) _amount;
   }

   private void updateUi() {
      final String qrText = getPaymentUri();

      if (_amount == null) {
         tvTitle.setText(R.string.bitcoin_address_title);
         btShare.setText(R.string.share_bitcoin_address);
         tvAmountLabel.setText(R.string.optional_amount);
         tvAmount.setText("");
      } else {
         tvTitle.setText(R.string.payment_request);
         btShare.setText(R.string.share_payment_request);
         tvAmountLabel.setText(R.string.amount_title);
         tvAmount.setText(
               Utils.getFormattedValueWithUnit(getBitcoinAmount(), _mbwManager.getBitcoinDenomination())
         );
      }

      // QR code
      ivQrCode.setQrCode(qrText);

      // Show warning if the record has no private key
      if (_havePrivateKey) {
         tvWarning.setVisibility(View.GONE);
      } else {
         tvWarning.setVisibility(View.VISIBLE);
      }

      String[] addressStrings = Utils.stringChopper(getBitcoinAddress(), 12);
      tvAddress1.setText(addressStrings[0]);
      tvAddress2.setText(addressStrings[1]);
      tvAddress3.setText(addressStrings[2]);

      updateAmount();
   }

   private void updateAmount() {
      if (_amount == null) {
         // No amount to show
         tvAmount.setText("");
      } else {
         // Set Amount
         tvAmount.setText(
               Utils.getFormattedValueWithUnit(getBitcoinAmount(), _mbwManager.getBitcoinDenomination())
         );
      }
   }

   private String getPaymentUri() {
      final StringBuilder uri = new StringBuilder("bitcoin:");
      uri.append(getBitcoinAddress());
      if (_amount != null) {
         uri.append("?amount=").append(CoinUtil.valueString(getBitcoinAmount().getLongValue(), false));
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
      if (CurrencyValue.isNullOrZero(_amount)) {
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
         _amount = (CurrencyValue) intent.getSerializableExtra(GetAmountActivity.AMOUNT);
      } else {
         super.onActivityResult(requestCode, resultCode, intent);
      }
   }

   @OnClick(R.id.btEnterAmount)
   public void onEnterClick() {
      if (_amount == null) {
         GetAmountActivity.callMe(ReceiveCoinsActivity.this, null, GET_AMOUNT_RESULT_CODE);
      } else {
         // call the amount activity with the exact amount, so that the user sees the same amount he had entered
         // it in non-BTC
         GetAmountActivity.callMe(ReceiveCoinsActivity.this, _amount.getExactValueIfPossible(), GET_AMOUNT_RESULT_CODE);
      }
   }
}
