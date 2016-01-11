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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import butterknife.*;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.SignedMessage;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.bitid.ExternalService;
import com.mycelium.wallet.external.cashila.ApiException;
import com.mycelium.wallet.external.cashila.api.CashilaService;
import com.mycelium.wallet.external.cashila.api.request.CashilaAccountLoginRequest;
import com.mycelium.wallet.external.cashila.api.request.CashilaAccountRequest;
import com.mycelium.wallet.external.cashila.api.response.CashilaAccountResponse;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;


public class CashilaSignUpActivity extends Activity {

   private static final String CASHILA_SIGN_UP_SERVICE = "cashila_signup";
   @InjectView(R.id.cbAgreeCashilaTos) CheckBox cbAgreeCashilaTos;
   @InjectView(R.id.cbAgreeMyceliumTos) CheckBox cbAgreeMyceliumTos;
   @InjectView(R.id.rbUseExistingAccount) RadioButton rbUseExistingAccount;
   @InjectView(R.id.rbCreateNewAccount) RadioButton rbCreateNewAccount;
   @InjectView(R.id.etLoginEmailAddress) EditText etLoginEmailAddress;
   @InjectView(R.id.etPassword) EditText etPassword;
   @InjectView(R.id.et2Fa) EditText et2Fa;
   @InjectView(R.id.etSignUpEmailAddress) EditText etSignUpEmailAddress;
   @InjectView(R.id.llLogin) LinearLayout llLogin;
   @InjectView(R.id.llSignUp) LinearLayout llSignUp;
   @InjectView(R.id.btSignUp) Button btSignUp;
   @InjectView(R.id.btLogin) Button btLogin;
   @InjectView(R.id.tvCashilaTosLink) TextView tvCashilaTosLink;
   @InjectView(R.id.tvMyceliumTosLink) TextView tvMyceliumTosLink;

   private CashilaService cs;
   private MbwManager mbw;
   private String cashilaServiceUrl;

   public static Intent getIntent(Context context) {
      return new Intent(context, CashilaSignUpActivity.class);
   }

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.ext_cashila_signup_account);
      ButterKnife.inject(this);

      mbw = MbwManager.getInstance(this);
      mbw.getEventBus().register(this);


      try {
         cs = (CashilaService) mbw.getBackgroundObjectsCache().get(CASHILA_SIGN_UP_SERVICE, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
               String api = ExternalService.CASHILA.getApi(mbw.getNetwork());
               return new CashilaService(api, mbw.getEventBus());
            }
         });
      } catch (ExecutionException e) {
         throw new RuntimeException(e);
      }

      cashilaServiceUrl = ExternalService.CASHILA.getHost(mbw.getNetwork());

      this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);


      ClickSpan.clickify(tvCashilaTosLink, onClickCashilaTos);
      ClickSpan.clickify(tvMyceliumTosLink, onClickMyceliumTos);

      // prevent the user from checking the box without reading the TOS
      cbAgreeCashilaTos.setTag(false);
      cbAgreeMyceliumTos.setTag(false);


      setEnabled();
      setVisibility();
   }

   private void doSignUp() {
      final InMemoryPrivateKey privKey = getIdKey();
      final Address address = privKey.getPublicKey().toAddress(mbw.getNetwork());

      String email = etSignUpEmailAddress.getText().toString();
      cs.createNewAccount(new CashilaAccountRequest(email), new CashilaService.BitIdSignatureProvider() {
         @Override
         public CashilaAccountRequest.BitId signChallenge(String challenge) {
            final SignedMessage signedMessage = privKey.signMessage(challenge);
            return new CashilaAccountRequest.BitId(address, challenge, signedMessage.getBase64Signature());
         }
      })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<CashilaAccountResponse>() {
               @Override
               public void onCompleted() {
               }

               @Override
               public void onError(Throwable e) {
                  handleApiError(e);
               }

               @Override
               public void onNext(CashilaAccountResponse cashilaAccountResponse) {
                  // Account login was successful, mark cashila as linked and return
                  savePairedServiceAndReturn();
               }
            });
   }

   private InMemoryPrivateKey getIdKey() {
      return cs.getIdKey(mbw);
   }

   private void doLogin() {
      final InMemoryPrivateKey privKey = getIdKey();
      final Address addr = privKey.getPublicKey().toAddress(mbw.getNetwork());

      String email = etLoginEmailAddress.getText().toString();
      String password = etPassword.getText().toString();
      String secondFactor = et2Fa.getText().toString();

      cs.loginExistingAccount(new CashilaAccountLoginRequest(email, password, secondFactor), new CashilaService.BitIdSignatureProvider() {
         @Override
         public CashilaAccountRequest.BitId signChallenge(String challenge) {
            final SignedMessage signedMessage = privKey.signMessage(challenge);
            return new CashilaAccountRequest.BitId(addr, challenge, signedMessage.getBase64Signature());
         }
      })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<CashilaAccountResponse>() {
               @Override
               public void onCompleted() {

               }

               @Override
               public void onError(Throwable e) {
                  handleApiError(e);
               }

               @Override
               public void onNext(CashilaAccountResponse cashilaAccountResponse) {
                  savePairedServiceAndReturn();

               }
            });
   }

   private void savePairedServiceAndReturn() {
      // Account login was successful, mark cashila as linked and return
      mbw.getMetadataStorage().setPairedService(cashilaServiceUrl, true);
      final Intent intent = CashilaPaymentsActivity.getIntent(CashilaSignUpActivity.this);
      startActivity(intent);
      CashilaSignUpActivity.this.finish();
   }

   void setEnabled() {
      String mailAddress = (
            rbCreateNewAccount.isChecked() ? etSignUpEmailAddress.getText() : etLoginEmailAddress.getText()
      ).toString();

      boolean pwdOkay = etPassword.getText().length() > 0;

      btSignUp.setEnabled(
            cbAgreeCashilaTos.isChecked() &&
                  cbAgreeMyceliumTos.isChecked() &&
                  mailAddress.length() > 0 &&
                  Utils.isValidEmailAddress(mailAddress)
      );

      btLogin.setEnabled(
            mailAddress.length() > 0 &&
                  Utils.isValidEmailAddress(mailAddress) &&
                  pwdOkay
      );

   }

   private void handleApiError(Throwable e) {
      if (e instanceof ApiException) {
         if (e instanceof ApiException.WrongPassword) {
            Utils.showSimpleMessageDialog(CashilaSignUpActivity.this, getString(R.string.cashila_error_wrong_password));
         } else if (e instanceof ApiException.WrongSecondFactor) {
            Utils.showSimpleMessageDialog(CashilaSignUpActivity.this, getString(R.string.cashila_error_wrong_second_factor));
         } else if (e instanceof ApiException.UserAlreadyExists) {
            Utils.showSimpleMessageDialog(CashilaSignUpActivity.this, getString(R.string.cashila_error_user_already_exists));
         } else {
            // generic api error message
            Utils.showSimpleMessageDialog(CashilaSignUpActivity.this, getString(R.string.cashila_error_generic, e.getMessage()));
         }
      } else {
         // not an API error - throw it and crash
         throw new RuntimeException(e);
      }
   }

   private ClickSpan.OnClickListener onClickCashilaTos = new ClickSpan.OnClickListener() {

      public boolean isRunning = false;

      @Override
      public void onClick() {
         // prevent if the user double taps to open more than one dialog
         if (isRunning) {
            return;
         }

         isRunning = true;
         final ProgressDialog progress = new ProgressDialog(CashilaSignUpActivity.this);
         progress.setCancelable(false);
         progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
         progress.setMessage(getString(R.string.cashila_fetching_tos));
         progress.show();
         cs.getTermsOfUse()
               .subscribeOn(Schedulers.io())
               .observeOn(AndroidSchedulers.mainThread())
               .subscribe(new Subscriber<String>() {
                  @Override
                  public void onCompleted() {
                     isRunning = false;
                  }

                  @Override
                  public void onError(Throwable e) {
                     progress.dismiss();
                     handleApiError(e);
                  }

                  @Override
                  public void onNext(String s) {
                     progress.dismiss();
                     showTos(CashilaSignUpActivity.this, s);
                     isRunning = false;
                  }
               });
      }
   };


   private void showTos(final Context context, String tos) {
      LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      final View layout = inflater.inflate(R.layout.large_text_message_dialog, null, false);
      AlertDialog.Builder builder = new AlertDialog.Builder(context).setView(layout);
      final AlertDialog dialog = builder.create();
      TextView tvMessage = ((TextView) layout.findViewById(R.id.tvMessage));
      tvMessage.setText(tos);

      dialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
         @Override
         public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            cbAgreeCashilaTos.setTag(true);
         }
      });
      dialog.show();
   }

   private ClickSpan.OnClickListener onClickMyceliumTos = new ClickSpan.OnClickListener() {
      @Override
      public void onClick() {
         showTos(CashilaSignUpActivity.this, getString(R.string.cashila_mycelium_limited_liability));
         // reset the checked state after the user clicked a inside-text link
         cbAgreeMyceliumTos.setTag(true);
      }
   };

   @OnClick(R.id.rbUseExistingAccount)
   void onClickUseExisting() {
      setVisibility();
   }

   @OnClick(R.id.rbCreateNewAccount)
   void onClickCreateNew() {
      setVisibility();
   }

   @OnClick(R.id.btCancel)
   void onClickCancel() {
      setResult(RESULT_CANCELED);
      finish();
   }

   @OnClick(R.id.btSignUp)
   void onClickSignUp() {
      doSignUp();
   }

   @OnClick(R.id.btLogin)
   void onClickLogin() {
      doLogin();
   }

   @OnTextChanged(R.id.etLoginEmailAddress)
   void onTextChangeLoginMail() {
      setEnabled();
   }

   @OnTextChanged(R.id.etSignUpEmailAddress)
   void onTextChangeSignUpMail() {
      setEnabled();
   }

   @OnTextChanged(R.id.etPassword)
   void onTextChangePassword() {
      setEnabled();
   }

   @OnCheckedChanged(R.id.cbAgreeCashilaTos)
   void onCheckedCashilaTos() {
      if (!((Boolean) cbAgreeCashilaTos.getTag())) {
         cbAgreeCashilaTos.setChecked(false);
         Toast.makeText(this, "Please read the associated TOS first", Toast.LENGTH_SHORT).show();
      }
      setEnabled();
   }

   @OnCheckedChanged(R.id.cbAgreeMyceliumTos)
   void onCheckedMycelium() {
      if (!((Boolean) cbAgreeMyceliumTos.getTag())) {
         cbAgreeMyceliumTos.setChecked(false);
         Toast.makeText(this, "Please read the associated TOS first", Toast.LENGTH_SHORT).show();
      }
      setEnabled();
   }

   @OnCheckedChanged(R.id.rbCreateNewAccount)
   void onCheckedNewAccount() {
      setEnabled();
   }


   @OnCheckedChanged(R.id.rbUseExistingAccount)
   void onCheckedExisting() {
      setEnabled();
   }


   void setVisibility() {
      llLogin.setVisibility(rbUseExistingAccount.isChecked() ? View.VISIBLE : View.GONE);
      llSignUp.setVisibility(rbCreateNewAccount.isChecked() ? View.VISIBLE : View.GONE);
      btLogin.setVisibility(llLogin.getVisibility());
      btSignUp.setVisibility(llSignUp.getVisibility());
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
   }


   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      return false;
   }

   public static class ClickSpan extends ClickableSpan {

      private OnClickListener mListener;

      public ClickSpan(OnClickListener listener) {
         mListener = listener;
      }

      @Override
      public void onClick(View widget) {
         if (mListener != null) {
            mListener.onClick();
         }
      }

      public interface OnClickListener {
         void onClick();
      }

      public static void clickify(TextView tv, final OnClickListener listener) {
         SpannableString s = SpannableString.valueOf(tv.getText());
         ClickSpan span = new ClickSpan(listener);
         s.setSpan(span, 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
         tv.setText(s);

         MovementMethod m = tv.getMovementMethod();
         if ((m == null) || !(m instanceof LinkMovementMethod)) {
            tv.setMovementMethod(LinkMovementMethod.getInstance());
         }
      }
   }
}

