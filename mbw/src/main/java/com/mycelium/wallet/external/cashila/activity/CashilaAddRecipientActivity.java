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

package com.mycelium.wallet.external.cashila.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import com.google.common.base.Strings;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.bitid.ExternalService;
import com.mycelium.wallet.external.cashila.ApiException;
import com.mycelium.wallet.external.cashila.api.CashilaService;
import com.mycelium.wallet.external.cashila.api.request.SaveRecipient;
import com.mycelium.wallet.external.cashila.api.response.BillPayExistingRecipient;
import com.mycelium.wallet.external.cashila.api.response.SupportedCountries;
import nl.garvelink.iban.IBAN;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;


public class CashilaAddRecipientActivity extends Activity {
   public static final String RECIPIENT_ID = "RECIPIENT_ID";
   public static final String SELECTED_COUNTRY = "SELECTED_COUNTRY";

   @BindView(R.id.etName) EditText etName;
   @BindView(R.id.etAddress) EditText etAddress;
   @BindView(R.id.etPostalCode) EditText etPostalCode;
   @BindView(R.id.etCity) EditText etCity;
   @BindView(R.id.etIban) EditText etIban;
   @BindView(R.id.etBic) EditText etBic;
   @BindView(R.id.spCountries) Spinner spCountries;
   private CashilaService cs;
   private MbwManager mbw;
   private UUID recipientId;

   public static Intent getIntent(Context context) {
      return new Intent(context, CashilaAddRecipientActivity.class);
   }

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.ext_cashila_add_recipient);
      ButterKnife.bind(this);

      mbw = MbwManager.getInstance(this);
      mbw.getEventBus().register(this);


      try {
         cs = (CashilaService) mbw.getBackgroundObjectsCache().get(CashilaPaymentsActivity.CASHILA_SERVICE, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
               String api = ExternalService.CASHILA.getApi(mbw.getNetwork());
               return new CashilaService(api, mbw.getEventBus());
            }
         });
      } catch (ExecutionException e) {
         throw new RuntimeException(e);
      }

      // restore the previous selected country and recpient ID
      int selCountry = -1;
      if (savedInstanceState != null) {
         recipientId = (UUID) savedInstanceState.getSerializable(RECIPIENT_ID);
         selCountry = savedInstanceState.getInt(SELECTED_COUNTRY, 0);
      }

      // if no recipient ID was chosen so far, generate a new random one
      if (recipientId == null) {
         recipientId = UUID.randomUUID();
      }

      this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

      getCountries(selCountry);
      IbanTextFormatter.watch(etIban);
   }

   private void getCountries(final int selected) {
      cs.getSupportedCountries()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<SupportedCountries>() {
               @Override
               public void onCompleted() {
               }

               @Override
               public void onError(Throwable e) {
                  handleApiError(e);
               }

               @Override
               public void onNext(SupportedCountries countries) {
                  final ArrayAdapter<SupportedCountries.Country> adapter =
                        new ArrayAdapter<SupportedCountries.Country>(
                              CashilaAddRecipientActivity.this,
                              R.layout.ext_cashila_country_row,
                              R.id.tvCountryName,
                              countries
                        );
                  spCountries.setAdapter(adapter);
                  if (selected >= 0) {
                     // select country by list position
                     spCountries.setSelection(selected, false);
                  } else {
                     // select country by country code
                     String countyCode = mbw.getMetadataStorage().getCashilaLastUsedCountryCode();
                     int count = 0;
                     for (SupportedCountries.Country country : countries) {
                        if (country.code.equals(countyCode)) {
                           spCountries.setSelection(count);
                        }
                        count++;
                     }
                  }
               }
            });
   }

   private void handleApiError(Throwable e) {
      if (e instanceof ApiException) {
         // generic api error message
         Utils.showSimpleMessageDialog(CashilaAddRecipientActivity.this, getString(R.string.cashila_error, e.getMessage()));
      } else {
         // not an API error - throw it and crash
         throw new RuntimeException(e);
      }
   }

   @OnEditorAction(R.id.etBic) boolean onEditorActionBic(int actionId){
      if(actionId== EditorInfo.IME_ACTION_DONE){
         onClickOk();
         return true;
      }
      return false;
   }

   @OnClick(R.id.btOk)
   void onClickOk() {
      final SupportedCountries.Country selectedCountry = (SupportedCountries.Country) spCountries.getSelectedItem();
      if (selectedCountry == null) {
         return;
      }
      final String name = etName.getText().toString();
      final String address = etAddress.getText().toString();
      final String postalCode = etPostalCode.getText().toString();
      final String city = etCity.getText().toString();
      final String code = selectedCountry.code;
      final String iban = etIban.getText().toString();
      final String bic = etBic.getText().toString();

      if (Strings.isNullOrEmpty(name) ||
            Strings.isNullOrEmpty(address) ||
            Strings.isNullOrEmpty(postalCode) ||
            Strings.isNullOrEmpty(city) ||
            Strings.isNullOrEmpty(code) ||
            Strings.isNullOrEmpty(iban)
            ) {
         // bic is semi-optional, in most cases cashila can derive it from the iban
         // if not, the api will return an error, which we will show to the user
         Toast.makeText(this, R.string.cashila_fill_out_all_fields, Toast.LENGTH_LONG).show();
         return;
      }

      final SaveRecipient saveRecipient = new SaveRecipient(
            name,
            address,
            postalCode,
            city,
            code,
            iban,
            bic
      );
      saveRecipient(saveRecipient);
   }

   @OnClick(R.id.btCancel)
   void onClickCancel() {
      setResult(RESULT_CANCELED);
      finish();
   }

   private void saveRecipient(SaveRecipient saveRecipient) {
      cs.saveRecipient(recipientId, saveRecipient)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<BillPayExistingRecipient>() {
               @Override
               public void onCompleted() {

               }

               @Override
               public void onError(Throwable e) {
                  handleApiError(e);
               }

               @Override
               public void onNext(BillPayExistingRecipient recipient) {
                  if (recipient != null && recipient.id != null) {

                     // save the chosen country code locally, to ease the selection next time the
                     // user tries to add one again.
                     mbw.getMetadataStorage().setCashilaLastUsedCountryCode(recipient.countryCode);

                     Toast.makeText(CashilaAddRecipientActivity.this, R.string.cashila_new_recipient_saved, Toast.LENGTH_SHORT).show();
                     final Intent result = new Intent()
                           .putExtra(RECIPIENT_ID, UUID.fromString(recipient.id));
                     CashilaAddRecipientActivity.this.setResult(RESULT_OK, result);
                     CashilaAddRecipientActivity.this.finish();
                  }
               }
            });
   }

   @Override
   protected void onDestroy() {
      super.onDestroy();
      mbw.getEventBus().unregister(this);
   }

   @Override
   protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putSerializable(RECIPIENT_ID, recipientId);
      outState.putSerializable(SELECTED_COUNTRY, spCountries.getSelectedItemPosition());
   }


   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      return false;
   }

   private static class IbanTextFormatter implements TextWatcher {
      private final TextView textView;
      private boolean ignoreChange = false;

      public IbanTextFormatter(TextView textView) {
         this.textView = textView;
      }

      @Override
      final public void afterTextChanged(Editable s) {
         if (ignoreChange) {
            return;
         }

         String text = textView.getText().toString();
         //validate(textView, text);
         try {
            final IBAN iban = IBAN.valueOf(text);
            ignoreChange = true;
            // format the text
            textView.setText(iban.toString());

         } catch (IllegalArgumentException e) {
            //Toast.makeText(textView.getContext(), "IBAN not valid", Toast.LENGTH_LONG).show();
         }
      }

      @Override
      final public void beforeTextChanged(CharSequence s, int start, int count, int after) {  }

      @Override
      final public void onTextChanged(CharSequence s, int start, int before, int count) {  }

      public static void watch(EditText editable) {
         editable.addTextChangedListener(new IbanTextFormatter(editable));
      }
   }
}

