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
package com.mycelium.wallet.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.NetworkConnectionWatcher.ConnectionObserver;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.SimpleGestureFilter;
import com.mycelium.wallet.SimpleGestureFilter.SimpleGestureListener;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.addressbook.AddressBookActivity;
import com.mycelium.wallet.activity.receive.WithAmountActivity;
import com.mycelium.wallet.activity.send.SendActivityHelper;
import com.mycelium.wallet.api.AbstractCallbackHandler;
import com.mycelium.wallet.api.AndroidAsyncApi;
import com.mycelium.wallet.api.ApiCache;
import com.mycelium.wallet.api.AsyncTask;
import com.mrd.mbwapi.api.ApiError;
import com.mrd.mbwapi.api.Balance;
import com.mrd.mbwapi.api.ExchangeSummary;

public class BalanceActivity extends Activity implements ConnectionObserver, SimpleGestureListener {

   private Record _record;
   private AsyncTask _task;
   private Balance _balance;
   private Double _oneBtcInFiat;
   private ApiCache _cache;
   private SimpleGestureFilter _gestureFilter;
   private AlertDialog _qrCodeDialog;
   private AlertDialog _hintDialog;
   private Handler _hintHandler;
   private AddressBookManager _addressBook;
   private MbwManager _mbwManager;

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.balance_activity);
      _mbwManager = MbwManager.getInstance(this.getApplication());
      _cache = _mbwManager.getCache();
      _addressBook = _mbwManager.getAddressBookManager();

      _record = _mbwManager.getRecordManager().getSelectedRecord();
      if (!_record.hasPrivateKey()) {
         findViewById(R.id.btSend).setVisibility(View.GONE);
         findViewById(R.id.vSendGap).setVisibility(View.GONE);
      }

      final ImageView qrImage = (ImageView) findViewById(R.id.ivQR);

      // Show small QR code once the layout has completed
      qrImage.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

         @Override
         public void onGlobalLayout() {
            int height = qrImage.getHeight();
            Bitmap qrCode = Utils.getQRCodeBitmap("bitcoin:" + _record.address.toString(), height, 0);
            qrImage.setImageBitmap(qrCode);
         }
      });

      // Show large QR code when clicking small qr code
      qrImage.setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View v) {
            showQrCode();
         }
      });

      findViewById(R.id.llAddress).setOnClickListener(addressClickListener);

      findViewById(R.id.llBalance).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View v) {
            refresh();
         }
      });

      findViewById(R.id.btSend).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            SendActivityHelper.startSendActivity(BalanceActivity.this, _record, null, null);
         }
      });

      findViewById(R.id.btReceive).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            Intent intent = new Intent(BalanceActivity.this, WithAmountActivity.class);
            startActivity(intent);
         }
      });

      // Set address
      String[] addressStrings = Utils.stringChopper(_record.address.toString(), 12);
      ((TextView) findViewById(R.id.tvAddress1)).setText(addressStrings[0]);
      ((TextView) findViewById(R.id.tvAddress2)).setText(addressStrings[1]);
      ((TextView) findViewById(R.id.tvAddress3)).setText(addressStrings[2]);

      // Set beta build
      PackageInfo pInfo;
      try {
         pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
         ((TextView) findViewById(R.id.tvBetaBuild)).setText(getResources().getString(R.string.beta_build,
               pInfo.versionName));
      } catch (NameNotFoundException e) {
         // Ignore
      }

      // Set hint button text manually as android doesn't like '?'
      ((Button) findViewById(R.id.btHint)).setText("?");

   }

   @Override
   protected void onDestroy() {
      if (_qrCodeDialog != null && _qrCodeDialog.isShowing()) {
         _qrCodeDialog.dismiss();
      }
      if (_hintDialog != null && _hintDialog.isShowing()) {
         _hintDialog.dismiss();
      }
      cancelEverything();
      super.onDestroy();
   }

   @Override
   protected void onResume() {
      if (!Utils.isConnected(this)) {
         Utils.toastConnectionError(this);
      }
      updateLabel();
      refresh();
      _gestureFilter = new SimpleGestureFilter(this, this);
      // Register for network going up/down callbacks
      MbwManager.getInstance(this.getApplication()).getNetworkConnectionWatcher().addObserver(this);
      animateSwipe();

      // Delay hints by a few seconds
      _hintHandler = new Handler();
      _hintHandler.postDelayed(delayedHint, 5000);
      super.onResume();
   }

   private void animateSwipe() {
      long speed = 500;
      long delay = 200;
      Utils.fadeViewInOut(findViewById(R.id.tvLeftArrow4), delay * 3, speed, 0);
      Utils.fadeViewInOut(findViewById(R.id.tvLeftArrow3), delay * 2, speed, 0);
      Utils.fadeViewInOut(findViewById(R.id.tvLeftArrow2), delay * 1, speed, 0);
      Utils.fadeViewInOut(findViewById(R.id.tvLeftArrow1), delay * 0, speed, 0);
      Utils.fadeViewInOut(findViewById(R.id.tvRightArrow1), delay * 0, speed, 0);
      Utils.fadeViewInOut(findViewById(R.id.tvRightArrow2), delay * 1, speed, 0);
      Utils.fadeViewInOut(findViewById(R.id.tvRightArrow3), delay * 2, speed, 0);
      Utils.fadeViewInOut(findViewById(R.id.tvRightArrow4), delay * 3, speed, 0);
   }

   @Override
   protected void onPause() {
      // Unregister for network going up/down callbacks
      MbwManager.getInstance(this.getApplication()).getNetworkConnectionWatcher().removeObserver(this);
      if (_hintHandler != null) {
         _hintHandler.removeCallbacks(delayedHint);
      }
      super.onPause();
   }

   private void cancelEverything() {
      if (_task != null) {
         _task.cancel();
      }
   }

   private void refresh() {
      if (_task != null) {
         return;
      }

      // Show cached balance and progress spinner
      findViewById(R.id.pbBalance).setVisibility(View.VISIBLE);
      _balance = _cache.getBalance(_record.address);
      updateBalance();

      // Create a task for getting the current balance
      AndroidAsyncApi api = _mbwManager.getAsyncApi();
      _task = api.getBalance(_record.address, new QueryBalanceHandler());

   }

   private void updateLabel() {
      // Show name of bitcoin address according to address book
      TextView tvAddressTitle = (TextView) findViewById(R.id.tvAddressLabel);
      String name = _addressBook.getNameByAddress(_record.address.toString());
      if (name.length() == 0) {
         tvAddressTitle.setText(R.string.your_bitcoin_address);
         tvAddressTitle.setGravity(Gravity.LEFT);
      } else {
         tvAddressTitle.setText(name);
         tvAddressTitle.setGravity(Gravity.CENTER_HORIZONTAL);
         tvAddressTitle.setGravity(Gravity.LEFT);
      }

   }

   private void updateBalance() {
      if (_balance == null) {
         return;
      }

      // Set Balance
      ((TextView) findViewById(R.id.tvBalance)).setText(_mbwManager.getBtcValueString(_balance.unspent
            + _balance.pendingChange));

      // Set Receiving
      String receivingString = _mbwManager.getBtcValueString(_balance.pendingReceiving);
      String receivingText = getResources().getString(R.string.receiving, receivingString);
      ((TextView) findViewById(R.id.tvReceiving)).setText(receivingText);

      // Set Sending
      String sendingString = _mbwManager.getBtcValueString(_balance.pendingSending);
      String sendingText = getResources().getString(R.string.sending, sendingString);
      ((TextView) findViewById(R.id.tvSending)).setText(sendingText);

      // Set Fiat value
      if (_oneBtcInFiat == null) {
         findViewById(R.id.tvFiat).setVisibility(View.INVISIBLE);
      } else {
         TextView tvFiat = (TextView) findViewById(R.id.tvFiat);
         tvFiat.setVisibility(View.VISIBLE);

         Double converted = Utils.getFiatValue(_balance.unspent + _balance.pendingChange, _oneBtcInFiat);
         String currency = MbwManager.getInstance(getApplication()).getFiatCurrency();
         tvFiat.setText(getResources().getString(R.string.approximate_fiat_value, currency, converted));

      }
      
      // Set BTC rate
      if (_oneBtcInFiat == null) {
         findViewById(R.id.tvBtcRate).setVisibility(View.INVISIBLE);
      } else {
         TextView tvBtcRate = (TextView) findViewById(R.id.tvBtcRate);
         tvBtcRate.setVisibility(View.VISIBLE);

         String currency = MbwManager.getInstance(getApplication()).getFiatCurrency();
         tvBtcRate.setText(getResources().getString(R.string.btc_rate, currency, _oneBtcInFiat));

      }
      
   }

   class QueryBalanceHandler implements AbstractCallbackHandler<Balance> {

      @Override
      public void handleCallback(Balance response, ApiError exception) {
         if (exception != null) {
            Utils.toastConnectionError(BalanceActivity.this);
            _task = null;
            _balance = null;
            _oneBtcInFiat = null;
         } else {
            _balance = response;
            updateBalance();
            AndroidAsyncApi api = _mbwManager.getAsyncApi();
            _task = api.getExchangeSummary(_mbwManager.getFiatCurrency(), new QueryExchangeSummaryHandler());
         }
      }

   }

   class QueryExchangeSummaryHandler implements AbstractCallbackHandler<ExchangeSummary[]> {

      @Override
      public void handleCallback(ExchangeSummary[] response, ApiError exception) {
         findViewById(R.id.pbBalance).setVisibility(View.INVISIBLE);
         if (exception != null) {
            Utils.toastConnectionError(BalanceActivity.this);
            _task = null;
            _oneBtcInFiat = null;
         } else {
            _oneBtcInFiat =  Utils.getLastTrade(response);
            updateBalance();
            _task = null;
         }
      }

   }

   /** Called when menu button is pressed. */
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.balance_options_menu, menu);
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      if (item.getItemId() == R.id.miSetLabel) {
         Utils.showSetAddressLabelDialog(this, _addressBook, _record.address.toString(), new Runnable() {

            @Override
            public void run() {
               updateLabel();
            }
         });
         return true;
      } else if (item.getItemId() == R.id.miSettings) {
         Intent intent = new Intent(BalanceActivity.this, SettingsActivity.class);
         startActivity(intent);
         return true;
      } else if (item.getItemId() == R.id.miAddressBook) {
         Intent intent = new Intent(BalanceActivity.this, AddressBookActivity.class);
         startActivity(intent);
         return true;
      }
      return super.onOptionsItemSelected(item);
   }

   private void showQrCode() {
      String address = "bitcoin:" + _record.address.toString();
      MbwManager manager = MbwManager.getInstance(this.getApplication());
      Bitmap bitmap = Utils.getLargeQRCodeBitmap(address, manager);
      _qrCodeDialog = Utils.showQrCode(BalanceActivity.this, R.string.bitcoin_address_title, bitmap,
            _record.address.toString(), R.string.copy_address_to_clipboard);
   }

   @Override
   public void OnNetworkConnected() {
      if (isFinishing()) {
         return;
      }
      new Handler().post(new Runnable() {
         @Override
         public void run() {
            refresh();
         }
      });
   }

   @Override
   public void OnNetworkDisconnected() {

   }

   @Override
   public void onSwipe(int direction) {
      if (direction == SimpleGestureFilter.SWIPE_LEFT) {
         Intent intent = new Intent(BalanceActivity.this, TransactionHistoryActivity.class);
         intent.putExtra("record", _record);
         startActivity(intent);
         this.overridePendingTransition(R.anim.right_to_left_enter, R.anim.right_to_left_exit);
      } else if (direction == SimpleGestureFilter.SWIPE_RIGHT) {
         Intent intent = new Intent(BalanceActivity.this, RecordsActivity.class);
         startActivity(intent);
         finish();
         this.overridePendingTransition(R.anim.left_to_right_enter, R.anim.left_to_right_exit);
      }
   }

   @Override
   public void onDoubleTap() {
   }

   @Override
   public boolean dispatchTouchEvent(MotionEvent me) {
      this._gestureFilter.onTouchEvent(me);
      return super.dispatchTouchEvent(me);
   }

   private Runnable delayedHint = new Runnable() {

      @Override
      public void run() {
         if (_mbwManager.getHintManager().timeForAHint()) {
            _hintDialog = _mbwManager.getHintManager().showHint(BalanceActivity.this);
         }
      }
   };

   private final OnClickListener addressClickListener = new OnClickListener() {

      @Override
      public void onClick(View v) {
         Intent intent = new Intent(Intent.ACTION_SEND);
         intent.setType("text/plain");
         intent.putExtra(Intent.EXTRA_TEXT, _mbwManager.getRecordManager().getSelectedRecord().address.toString());
         startActivity(Intent.createChooser(intent, getString(R.string.share_bitcoin_address)));
      }
   };

}