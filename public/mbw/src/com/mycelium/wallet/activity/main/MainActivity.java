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

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.Record.Tag;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.Wallet;
import com.mycelium.wallet.Wallet.BalanceInfo;
import com.mycelium.wallet.WalletMode;
import com.mycelium.wallet.activity.KeyVulnerabilityDialog;
import com.mycelium.wallet.activity.RecordsActivity;
import com.mycelium.wallet.activity.addressbook.AddressBookActivity;
import com.mycelium.wallet.activity.main.BalanceFragment.BalanceFragmentContainer;
import com.mycelium.wallet.activity.main.TransactionHistoryFragment.TransactionHistoryFragmentContainer;
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity;
import com.mycelium.wallet.activity.send.InstantWalletActivity;
import com.mycelium.wallet.activity.settings.SettingsActivity;

public class MainActivity extends FragmentActivity implements BalanceFragmentContainer,
      TransactionHistoryFragmentContainer {

   /**
    * The pager widget, which handles swiping horizontally between balance view
    * and transaction history view.
    */
   private ViewPager _pager;
   private ScreenSlidePagerAdapter _pagerAdapter;
   private MbwManager _mbwManager;
   private AddressBookManager _addressBook;
   private int _globalLayoutHeight;
   private Wallet _wallet;
   private Dialog _weakKeyDialog;
   private Handler _hintHandler;
   private AlertDialog _hintDialog;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main_activity);

      // Instantiate a ViewPager and a PagerAdapter.
      _pager = (ViewPager) findViewById(R.id.pager);
      _pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
      _pager.setAdapter(_pagerAdapter);

      _mbwManager = MbwManager.getInstance(this.getApplication());
      _addressBook = _mbwManager.getAddressBookManager();

      // Show small QR code once the layout has completed
      final ImageView qrImage = (ImageView) findViewById(R.id.ivQR);
      qrImage.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

         @Override
         public void onGlobalLayout() {
            int margin = 5;
            int height = qrImage.getHeight();
            // Guard to prevent us from drawing all the time
            if (_globalLayoutHeight == height) {
               return;
            }
            _globalLayoutHeight = height;

            Bitmap qrCode = Utils
                  .getQRCodeBitmap("bitcoin:" + _wallet.getReceivingAddress().toString(), height, margin);
            qrImage.setImageBitmap(qrCode);
         }
      });

      // Show large QR code when clicking small qr code
      qrImage.setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, ReceiveCoinsActivity.class);
            intent.putExtra("wallet", _wallet);
            startActivity(intent);
         }
      });

      findViewById(R.id.llAddress).setOnClickListener(addressClickListener);

      // Set beta build
      PackageInfo pInfo;
      try {
         pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
         ((TextView) findViewById(R.id.tvBetaBuild)).setText(getResources().getString(R.string.beta_build,
               pInfo.versionName));
      } catch (NameNotFoundException e) {
         // Ignore
      }

   }

   @Override
   protected void onResume() {
      initializeWallet();
      updateLabel();

      _weakKeyDialog = weakKeyCheck();

      if (_weakKeyDialog == null) {
         // Delay hints by a few seconds
         _hintHandler = new Handler();
         _hintHandler.postDelayed(delayedHint, 5000);
      }
      super.onResume();
   }

   @Override
   protected void onPause() {
      if (_hintHandler != null) {
         _hintHandler.removeCallbacks(delayedHint);
      }
      super.onPause();
   }

   @Override
   protected void onDestroy() {
      if (_hintDialog != null && _hintDialog.isShowing()) {
         _hintDialog.dismiss();
      }
      if (_weakKeyDialog != null && _weakKeyDialog.isShowing()) {
         _weakKeyDialog.dismiss();
      }
      super.onDestroy();
   }

   private void initializeWallet() {
      _wallet = _mbwManager.getRecordManager().getWallet(_mbwManager.getWalletMode());

      // Show/Hide notice about managing single archive key or single segregated
      // key
      boolean isArchivedKey = _mbwManager.getRecordManager().getSelectedRecord().tag == Tag.ARCHIVE;
      boolean isSegregatedKey = _mbwManager.getRecordManager().getSelectedRecord().tag == Tag.ACTIVE
            && _mbwManager.getWalletMode() == WalletMode.Segregated;
      TextView notice = (TextView) findViewById(R.id.tvKeyNotice);
      if (isArchivedKey) {
         notice.setText(R.string.managing_archive_key);
         notice.setVisibility(View.VISIBLE);
      } else if (isSegregatedKey) {
         notice.setText(R.string.managing_segregated_key);
         notice.setVisibility(View.VISIBLE);
      } else {
         notice.setVisibility(View.GONE);
      }

      // Set address
      String[] addressStrings = Utils.stringChopper(_wallet.getReceivingAddress().toString(), 12);
      ((TextView) findViewById(R.id.tvAddress1)).setText(addressStrings[0]);
      ((TextView) findViewById(R.id.tvAddress2)).setText(addressStrings[1]);
      ((TextView) findViewById(R.id.tvAddress3)).setText(addressStrings[2]);

   }

   @Override
   public void onBackPressed() {
      if (_pager.getCurrentItem() == 0) {
         // If the user is currently looking at the first page, allow the system
         // to handle the
         // Back button. This calls finish() on this activity and pops the back
         // stack.
         super.onBackPressed();
      } else {
         // Otherwise, select the balance view.
         _pager.setCurrentItem(_pager.getCurrentItem() - 1);
      }
   }

   private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

      private BalanceFragment _balanceFragment;
      private TransactionHistoryFragment _transactionHistoryFragment;

      public ScreenSlidePagerAdapter(FragmentManager fm) {
         super(fm);
         _balanceFragment = new BalanceFragment();
         _transactionHistoryFragment = new TransactionHistoryFragment();
      }

      @Override
      public Fragment getItem(int position) {
         if (position == 0) {
            return _balanceFragment;
         } else if (position == 1) {
            return _transactionHistoryFragment;
         } else {
            throw new RuntimeException("Invalid fragment position");
         }
      }

      public TransactionHistoryFragment getTransactionHistoryFragment() {
         return _transactionHistoryFragment;
      }

      @Override
      public int getCount() {
         return 2;
      }
   }

   private final OnClickListener addressClickListener = new OnClickListener() {

      @Override
      public void onClick(View v) {
         Intent intent = new Intent(Intent.ACTION_SEND);
         intent.setType("text/plain");
         intent.putExtra(Intent.EXTRA_TEXT, _wallet.getReceivingAddress().toString());
         startActivity(Intent.createChooser(intent, getString(R.string.share_bitcoin_address)));
      }
   };

   private void updateLabel() {
      // Show name of bitcoin address according to address book
      TextView tvAddressTitle = (TextView) findViewById(R.id.tvAddressLabel);
      String name = _addressBook.getNameByAddress(_wallet.getReceivingAddress().toString());
      if (name.length() == 0) {
         tvAddressTitle.setText(R.string.your_bitcoin_address);
         tvAddressTitle.setGravity(Gravity.LEFT);
      } else {
         tvAddressTitle.setText(name);
         tvAddressTitle.setGravity(Gravity.CENTER_HORIZONTAL);
         tvAddressTitle.setGravity(Gravity.LEFT);
      }

   }

   private Dialog weakKeyCheck() {
      List<Record> weakKeys = _mbwManager.getRecordManager().getWeakActiveKeys();
      if (weakKeys.isEmpty()) {
         return null;
      }

      KeyVulnerabilityDialog dialog = new KeyVulnerabilityDialog(this, weakKeys);
      dialog.show();
      return dialog;
   }

   private Runnable delayedHint = new Runnable() {

      @Override
      public void run() {
         if (_mbwManager.getHintManager().timeForAHint()) {
            _hintDialog = _mbwManager.getHintManager().showHint(MainActivity.this);
         }
      }
   };

   @Override
   public Wallet getWallet() {
      return _wallet;
   }

   @Override
   public MbwManager getMbwManager() {
      return _mbwManager;
   }

   @Override
   public void balanceChanged(BalanceInfo balance) {
      TransactionHistoryFragment fragment = _pagerAdapter.getTransactionHistoryFragment();
      fragment.refresh();
   }

   /**
    * Called when menu button is pressed.
    */
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.main_activity_options_menu, menu);
      return true;
   }

   @Override
   public boolean onPrepareOptionsMenu(Menu menu) {
      boolean expertMode = _mbwManager.getExpertMode();
      menu.findItem(R.id.miKeyManagement).setVisible(expertMode);
      menu.findItem(R.id.miExport).setVisible(!expertMode);
      return super.onPrepareOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      if (item.getItemId() == R.id.miSettings) {
         Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
         startActivity(intent);
         return true;
      } else if (item.getItemId() == R.id.miAddressBook) {
         Intent intent = new Intent(MainActivity.this, AddressBookActivity.class);
         startActivity(intent);
         return true;
      } else if (item.getItemId() == R.id.miKeyManagement) {
         Intent intent = new Intent(MainActivity.this, RecordsActivity.class);
         startActivity(intent);
         return true;
      } else if (item.getItemId() == R.id.miExport) {
         _mbwManager.runPinProtectedFunction(this, pinProtectedExport);
         return true;
      } else if (item.getItemId() == R.id.miColdStorage) {
         InstantWalletActivity.callMe(this);
         return true;
      }
      return super.onOptionsItemSelected(item);
   }

   final Runnable pinProtectedExport = new Runnable() {

      @Override
      public void run() {

         Record record = _mbwManager.getRecordManager().getSelectedRecord();
         if (record == null) {
            return;
         }
         Utils.exportPrivateKey(record, MainActivity.this);
      }
   };

}
