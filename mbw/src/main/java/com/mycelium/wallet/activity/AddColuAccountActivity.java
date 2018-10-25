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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.colu.ColuAccount.ColuAsset;
import com.mycelium.wallet.colu.ColuAccount.ColuAssetType;
import com.mycelium.wallet.colu.ColuManager;
import com.mycelium.wallet.event.AccountChanged;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.api.response.Feature;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.colu.PrivateColuConfig;
import com.mycelium.wapi.wallet.colu.coins.ColuMain;
import com.mycelium.wapi.wallet.colu.coins.MASSCoin;
import com.mycelium.wapi.wallet.colu.coins.MTCoin;
import com.mycelium.wapi.wallet.colu.coins.RMCCoin;
import com.squareup.otto.Bus;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;
import static com.mycelium.wallet.colu.ColuAccount.ColuAssetType.MASS;
import static com.mycelium.wallet.colu.ColuAccount.ColuAssetType.MT;
import static com.mycelium.wallet.colu.ColuAccount.ColuAssetType.RMC;

public class AddColuAccountActivity extends Activity {
   public static final int RESULT_COLU = 3;

   public static final String TAG = "AddColuAccountActivity";

   @BindView(R.id.btColuAddAccount) Button btColuAddAccount;
//   @BindView(R.id.tvTosLink) TextView tvTosLink;

   ColuMain selectedColuAsset;

   public static Intent getIntent(Context context) {
      Intent intent = new Intent(context, AddColuAccountActivity.class);
      return intent;
   }

   public static final String RESULT_KEY = "account";
   private MbwManager _mbwManager;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      getWindow().setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.add_colu_account_activity);
      _mbwManager = MbwManager.getInstance(this);
      ButterKnife.bind(this);
      btColuAddAccount.setText(getString(R.string.colu_create_account, ""));
//      setTosLink(tvTosLink);
   }

   void setButtonEnabled(){
         btColuAddAccount.setEnabled(true);
   }

//   private void setTosLink(TextView link) {
//      link.setClickable(true);
//      link.setMovementMethod(LinkMovementMethod.getInstance());
//      String linkUrl = getString(R.string.colu_tos_link_url);
//      String text = "<a href='" + linkUrl + "'> " + link.getText() + "</a>";
//      link.setText(Html.fromHtml(text));
//   }

   @OnClick(R.id.btColuAddAccount)
   void onColuAddAccountClick() {
      if(selectedColuAsset != null) {
         createColuAccountProtected(selectedColuAsset);
      } else {
         Toast.makeText(AddColuAccountActivity.this, R.string.colu_select_an_account_type, Toast.LENGTH_SHORT).show();
      }
     //displayTemporaryMessage();
   }

   @OnClick({R.id.radio_mycelium_tokens, R.id.radio_mass_tokens, R.id.radio_rmc_tokens})
   public void onRadioButtonClicked(View view) {
      // Is the button now checked?
      boolean checked = ((RadioButton) view).isChecked();
      ColuMain assetType;
      String name;
      // Check which radio button was clicked
      switch (view.getId()) {
         case R.id.radio_mycelium_tokens:
            assetType = MTCoin.INSTANCE;
            name = "MT";
            break;
         case R.id.radio_mass_tokens:
            assetType = MASSCoin.INSTANCE;
            name = "Mass";
            break;
         case R.id.radio_rmc_tokens:
            assetType = RMCCoin.INSTANCE;
            name = "RMC";
            break;
         default:
            return;
      }
      if (checked) {
         selectedColuAsset = assetType;
      }
      btColuAddAccount.setEnabled(true);
      Toast.makeText(this, name + " selected", Toast.LENGTH_SHORT).show();
   }

   private void createColuAccountProtected(final ColuMain coluAsset) {
      _mbwManager.getVersionManager().showFeatureWarningIfNeeded(
            AddColuAccountActivity.this, Feature.COLU_NEW_ACCOUNT, true, new Runnable() {
               @Override
               public void run() {
                  _mbwManager.runPinProtectedFunction(AddColuAccountActivity.this, new Runnable() {
                     @Override
                     public void run() {
                        new AddColuAsyncTask(_mbwManager.getEventBus(), coluAsset).execute();

                     }
                  });
               }
            }
      );
   }

   private class AddColuAsyncTask extends AsyncTask<Void, Integer, UUID> {
      private final boolean alreadyHadColuAccount;
      private Bus bus;
      private final ColuMain coluAsset;
      private final ProgressDialog progressDialog;

      public AddColuAsyncTask(Bus bus, ColuMain coluAsset) {
         this.bus = bus;
         this.coluAsset = coluAsset;
         this.alreadyHadColuAccount = _mbwManager.getMetadataStorage().isPairedService(MetadataStorage.PAIRED_SERVICE_COLU);
         progressDialog = ProgressDialog.show(AddColuAccountActivity.this, getString(R.string.colu), getString(R.string.colu_creating_account, coluAsset.getName()));
         progressDialog.setCancelable(false);
         progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
         progressDialog.show();
      }

      @Override
      protected UUID doInBackground(Void... params) {
         _mbwManager.getMetadataStorage().setPairedService(MetadataStorage.PAIRED_SERVICE_COLU, true);
         try {
            InMemoryPrivateKey key = new InMemoryPrivateKey(_mbwManager.getRandomSource(), true);
            UUID uuid = _mbwManager.getWalletManager(false)
                    .createAccounts(new PrivateColuConfig(key, coluAsset,AesKeyCipher.defaultKeyCipher())).get(0);
            return uuid;
         } catch (Exception e) {
            Log.d(TAG, "Error while creating Colored Coin account for asset " + coluAsset.getName() + ": " + e.getMessage());
            return null;
         }
      }

      @Override
      protected void onPostExecute(UUID account) {
         if (account != null) {
            Intent result = new Intent();
            result.putExtra(RESULT_KEY, account);
            setResult(RESULT_OK, result);
            finish();
         } else {
            // something went wrong - clean up the half ready coluManager
            Toast.makeText(AddColuAccountActivity.this, R.string.colu_unable_to_create_account, Toast.LENGTH_SHORT).show();
            _mbwManager.getMetadataStorage().setPairedService(MetadataStorage.PAIRED_SERVICE_COLU, alreadyHadColuAccount);
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
