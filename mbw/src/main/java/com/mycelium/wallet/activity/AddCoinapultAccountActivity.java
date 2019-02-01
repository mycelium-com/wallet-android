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

package com.mycelium.wallet.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.event.AccountChanged;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.api.response.Feature;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.coinapult.CoinapultConfig;
import com.mycelium.wapi.wallet.coinapult.Currency;
import com.squareup.otto.Bus;

import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.mycelium.wapi.wallet.coinapult.CoinapultModuleKt.getCoinapultAccount;

public class AddCoinapultAccountActivity extends Activity {
   public static final int RESULT_COINAPULT = 2;

   @BindView(R.id.btCoinapultAddGBP) Button btCoinapultAddGBP;
   @BindView(R.id.btCoinapultAddUSD) Button btCoinapultAddUSD;
   @BindView(R.id.btCoinapultAddEUR) Button btCoinapultAddEUR;
   @BindView(R.id.tvTosLink) TextView tvTosLink;

   public static Intent getIntent(Context context) {
      Intent intent = new Intent(context, AddCoinapultAccountActivity.class);
      //intent.putExtra("coinapult", addCoinapult);
      return intent;
   }

   public static final String RESULT_KEY = "account";
   private MbwManager _mbwManager;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.add_coinapult_account_activity);
      _mbwManager = MbwManager.getInstance(this);
      ButterKnife.bind(this);

      btCoinapultAddUSD.setText(getString(R.string.coinapult_currency_account, Currency.USD.getName()));
      btCoinapultAddEUR.setText(getString(R.string.coinapult_currency_account, Currency.EUR.getName()));
      btCoinapultAddGBP.setText(getString(R.string.coinapult_currency_account, Currency.GBP.getName()));

      setTosLink(tvTosLink);
   }

   void setButtonEnabled(){
      // only enable buttons for which we dont have an account already
      WalletManager walletManager = _mbwManager.getWalletManager(false);

      btCoinapultAddUSD.setEnabled(getCoinapultAccount(walletManager, Currency.USD) == null);
      btCoinapultAddEUR.setEnabled(getCoinapultAccount(walletManager, Currency.EUR) == null);
      btCoinapultAddGBP.setEnabled(getCoinapultAccount(walletManager, Currency.GBP) == null);
   }

   @OnClick(R.id.btCoinapultAddUSD)
   void onUsdClick() {
      createCoinapultAccountProtected(Currency.USD);
   }

   @OnClick(R.id.btCoinapultAddEUR)
   void onEurClick() {
      createCoinapultAccountProtected(Currency.EUR);
   }

   @OnClick(R.id.btCoinapultAddGBP)
   void onGbpClick() {
      createCoinapultAccountProtected(Currency.GBP);
   }


   private void createCoinapultAccountProtected(final Currency currency) {
      _mbwManager.getVersionManager().showFeatureWarningIfNeeded(
            AddCoinapultAccountActivity.this, Feature.COINAPULT_NEW_ACCOUNT, true, new Runnable() {
               @Override
               public void run() {
                  _mbwManager.runPinProtectedFunction(AddCoinapultAccountActivity.this, new Runnable() {
                     @Override
                     public void run() {
                        createCoinapultAccount(currency);
                     }
                  });
               }
            }
      );
   }

   private void createCoinapultAccount(final Currency currency) {
      AlertDialog.Builder b = new AlertDialog.Builder(this);
      b.setTitle(getString(R.string.coinapult));
      View diaView = getLayoutInflater().inflate(R.layout.ext_coinapult_tos, null);
      b.setView(diaView);
      b.setPositiveButton(getString(R.string.agree), new DialogInterface.OnClickListener() {
         @Override
         public void onClick(DialogInterface dialog, int which) {
            // Create the account initially without set email address
            // if needed, the user can later set and verify it via account menu.
            new AddCoinapultAsyncTask(_mbwManager.getEventBus(), Optional.<String>absent(), currency).execute();
         }
      });
      b.setNegativeButton(getString(R.string.dontagree), null);

      AlertDialog dialog = b.create();

      TextView link = (TextView) diaView.findViewById(R.id.tvTosLink);
      TextView tvThreshold = (TextView) diaView.findViewById(R.id.tvThreshold);
      tvThreshold.setText(getString(R.string.coinapult_threshold_warning, currency.getMinimumConversationString()));
      setTosLink(link);

      dialog.show();
   }

   private void setTosLink(TextView link) {
      link.setClickable(true);
      link.setMovementMethod(LinkMovementMethod.getInstance());
      String linkUrl = getString(R.string.coinapult_tos_link_url);
      String text = "<a href='" + linkUrl + "'> " + link.getText() + "</a>";
      link.setText(Html.fromHtml(text));
   }

   private class AddCoinapultAsyncTask extends AsyncTask<Void, Integer, UUID> {
      private final boolean alreadyHadCoinapultAccount;
      private Bus bus;
      private Optional<String> mail;
      private final Currency currency;
      private final ProgressDialog progressDialog;

      public AddCoinapultAsyncTask(Bus bus, Optional<String> mail, Currency currency) {
         this.bus = bus;
         this.mail = mail;
         this.currency = currency;
         this.alreadyHadCoinapultAccount = _mbwManager.getMetadataStorage().isPairedService(MetadataStorage.PAIRED_SERVICE_COINAPULT);
         progressDialog = ProgressDialog.show(AddCoinapultAccountActivity.this, getString(R.string.coinapult), getString(R.string.coinapult_create_account));
         progressDialog.setCancelable(false);
         progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
         progressDialog.show();
      }

      @Override
      protected UUID doInBackground(Void... params) {
         _mbwManager.getMetadataStorage().setPairedService(MetadataStorage.PAIRED_SERVICE_COINAPULT, true);
         String email = mail.isPresent() ? mail.get() : null;
         _mbwManager.getWalletManager(false).createAccounts(new CoinapultConfig(Currency.BTC, email));
         List<UUID> accounts = _mbwManager.getWalletManager(false).createAccounts(new CoinapultConfig(currency, email));
         return accounts.size() > 0 ? accounts.get(0) : null;
//         coinapultManager = _mbwManager.getCoinapultManager();
//         try {
//            coinapultManager.activateAccount(mail);
//            // save the mail address locally for later verification
//            if (mail.isPresent()) {
//               _mbwManager.getMetadataStorage().setCoinapultMail(mail.get());
//            }
//            UUID uuid = coinapultManager.enableCurrency(currency);
//            coinapultManager.scanForAccounts();
//            return uuid;
//         } catch (CoinapultClient.CoinapultBackendException e) {
//            return null;
//         }
      }

      @Override
      protected void onPostExecute(UUID account) {
         if (account != null) {
            bus.post(new AccountChanged(account));
            Intent result = new Intent();
            result.putExtra(RESULT_KEY, account);
            setResult(RESULT_COINAPULT, result);
            finish();
         } else {
            // something went wrong - clean up the half ready coinapultManager
            Toast.makeText(AddCoinapultAccountActivity.this, R.string.coinapult_unable_to_create_account, Toast.LENGTH_SHORT).show();
            _mbwManager.getMetadataStorage().setPairedService(MetadataStorage.PAIRED_SERVICE_COINAPULT, alreadyHadCoinapultAccount);
         }
         if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
         }
         setButtonEnabled();
      }
   }

   @Override
   public void onResume() {
      _mbwManager.getEventBus().register(this);
      setButtonEnabled();
      super.onResume();
   }

   @Override
   public void onPause() {
      _mbwManager.getEventBus().unregister(this);
      _mbwManager.getVersionManager().closeDialog();
      super.onPause();
   }
}
