package com.mycelium.wallet.external.glidera.fragments;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.external.glidera.GlideraUtils;
import com.mycelium.wallet.external.glidera.activities.GlideraTransaction;
import com.mycelium.wallet.external.glidera.api.GlideraService;
import com.mycelium.wallet.external.glidera.api.request.BuyPriceRequest;
import com.mycelium.wallet.external.glidera.api.request.BuyRequest;
import com.mycelium.wallet.external.glidera.api.response.BuyPriceResponse;
import com.mycelium.wallet.external.glidera.api.response.BuyResponse;
import com.mycelium.wallet.external.glidera.api.response.GlideraError;
import com.mycelium.wallet.external.glidera.api.response.TwoFactorResponse;
import com.mycelium.wapi.wallet.btc.WalletBtcAccount;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import rx.Observer;

public class GlideraBuy2faDialog extends DialogFragment {
   private MbwManager mbwManager;
   private GlideraService glideraService;
   private EditText et2FA;
   private TextView tvPurchaseSummary;
   private ProgressBar pbTimer;

   private String buyMode;
   private String btc;
   private String fiat;
   private String mode2FA;

   private volatile BuyPriceResponse _buyPriceResponse;

   public static GlideraBuy2faDialog newInstance(GlideraBuyFragment.BuyMode buyMode, BigDecimal btc, BigDecimal fiat, TwoFactorResponse
           .Mode mode) {
      Bundle bundle = new Bundle();
      bundle.putString("buyMode", buyMode.toString());
      bundle.putString("btc", btc.toPlainString());
      bundle.putString("fiat", fiat.toString());
      bundle.putString("mode2FA", mode.toString());

      GlideraBuy2faDialog glideraBuy2faDialog = new GlideraBuy2faDialog();
      glideraBuy2faDialog.setArguments(bundle);

      return glideraBuy2faDialog;
   }

   private void updatePrice() {
      BuyPriceRequest buyPriceRequest;
      if (buyMode.equals(GlideraBuyFragment.BuyMode.FIAT.toString())) {
         buyPriceRequest = new BuyPriceRequest(null, new BigDecimal(fiat));
      } else {
         buyPriceRequest = new BuyPriceRequest(new BigDecimal(btc), null);
      }

      glideraService.buyPrice(buyPriceRequest)
              .subscribe(new Observer<BuyPriceResponse>() {
                 @Override
                 public void onCompleted() {
                 }

                 @Override
                 public void onError(Throwable e) {
                 }

                 @Override
                 public void onNext(BuyPriceResponse buyPriceResponse) {
                    String purchaseSummary = "You are about to buy " + GlideraUtils.formatBtcForDisplay(buyPriceResponse.getQty()) +
                            " for " + GlideraUtils.formatFiatForDisplay(buyPriceResponse.getSubtotal()) + ".";
                    tvPurchaseSummary.setText(purchaseSummary);
                    _buyPriceResponse = buyPriceResponse;

                    //expire at the expiration time minus 10 seconds to act as a buffer
                    long expiration = buyPriceResponse.getExpires().getTime() - new Date().getTime() - 10000;

                    new Handler().postDelayed(new Runnable() {
                       @Override
                       public void run() {
                          updatePrice();
                       }
                    }, expiration);

                    pbTimer.clearAnimation();
                    ObjectAnimator animation = ObjectAnimator.ofInt(pbTimer, "progress", 0, 500); // see this max value coming back
                    animation.setDuration(expiration); //in milliseconds
                    animation.setInterpolator(new DecelerateInterpolator(.5f));
                    animation.start();
                 }
              });
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View root = Preconditions.checkNotNull(inflater.inflate(R.layout.glidera_dialog_2fa, container, false));

      tvPurchaseSummary = (TextView) root.findViewById(R.id.tvPurchaseSummary);
      et2FA = (EditText) root.findViewById(R.id.et2FA);
      pbTimer = (ProgressBar) root.findViewById(R.id.pbTimer);

      updatePrice();

      TextView tv2FASummary = (TextView) root.findViewById(R.id.tv2FASummary);
      Button buttonResend2FA = (Button) root.findViewById(R.id.buttonResend2FA);

      getDialog().setTitle("Confirm Your Purchase");

      if (mode2FA.equals(TwoFactorResponse.Mode.NONE.toString())) {
         tv2FASummary.setVisibility(View.GONE);
         buttonResend2FA.setVisibility(View.GONE);
         et2FA.setVisibility(View.GONE);
      } else if (mode2FA.equals(TwoFactorResponse.Mode.AUTHENTICATR.toString())) {
         String twoFASummary = "Please enter your 2-factor authorization (2FA) code from your Authenticator smartphone app to complete" +
                 " this purchase.";
         tv2FASummary.setText(twoFASummary);
         buttonResend2FA.setVisibility(View.GONE);
         et2FA.setHint("2FA Code");
      } else if (mode2FA.equals(TwoFactorResponse.Mode.PIN.toString())) {
         String twoFASummary = "Please enter your PIN to complete this purchase.";
         tv2FASummary.setText(twoFASummary);
         buttonResend2FA.setVisibility(View.GONE);
         et2FA.setHint("PIN");
      } else if (mode2FA.equals(TwoFactorResponse.Mode.SMS.toString())) {
         String twoFASummary = "A text message has been sent to your phone with a 2-factor authentication (2FA) code. Please enter it " +
                 "to confirm this purchase.";
         tv2FASummary.setText(twoFASummary);
         et2FA.setHint("2FA Code");
      }

      Button buttonCancel = (Button) root.findViewById(R.id.buttonCancel);
      buttonCancel.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            GlideraBuy2faDialog.this.getDialog().cancel();
         }
      });

      final Button buttonContinue = (Button) root.findViewById(R.id.buttonContinue);
      buttonContinue.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            if (_buyPriceResponse == null) {
               return;
            } else if (_buyPriceResponse.getExpires().before(new Date())) {
               updatePrice();
               return;
            }

            buttonContinue.setEnabled(false);
            final String twoFACode;

                /*
                Validate the 2fa or pin entered if two factor mode is anything other than none
                 */
            if (!mode2FA.equals(TwoFactorResponse.Mode.NONE.toString())) {
               twoFACode = et2FA.getText().toString();

               if (twoFACode.isEmpty()) {
                  if (mode2FA.equals(TwoFactorResponse.Mode.PIN.toString())) {
                     et2FA.setError("PIN is required");
                  } else {
                     et2FA.setError("2FA Code is required");
                  }
                  buttonContinue.setEnabled(true);
                  return;
               }
            } else {
               twoFACode = null;
            }

            Optional<Address> receivingAddress = ((WalletBtcAccount)(mbwManager.getSelectedAccount())).getReceivingAddress();
            if (receivingAddress.isPresent()) {
               Address address = receivingAddress.get();
               BigDecimal qty = _buyPriceResponse.getQty();
               UUID uuid = _buyPriceResponse.getPriceUuid();

               BuyRequest buyRequest = new BuyRequest(address, qty, uuid, false, null);

               glideraService.buy(buyRequest, twoFACode).subscribe(new Observer<BuyResponse>() {
                  @Override
                  public void onCompleted() {

                  }

                  @Override
                  public void onError(Throwable e) {
                     GlideraError error = GlideraService.convertRetrofitException(e);
                     if (error != null && error.getCode() != null) {
                        if (error.getCode() == GlideraError.ERROR_INCORRECT_PIN) {
                           if (mode2FA.equals(TwoFactorResponse.Mode.PIN.toString())) {
                              et2FA.setError("Incorrect PIN");
                           } else {
                              et2FA.setError("Incorrect 2FA Code");
                           }
                        }
                     }
                     buttonContinue.setEnabled(true);
                  }

                  @Override
                  public void onNext(BuyResponse buyResponse) {
                     Intent intent = new Intent(getActivity(), GlideraTransaction.class);
                     Bundle bundle = new Bundle();
                     bundle.putString("transactionuuid", buyResponse.getTransactionUuid().toString());
                     intent.putExtras(bundle);
                     startActivity(intent);
                  }
               });
            }
         }
      });

      buttonResend2FA.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            glideraService.getTwoFactor()
                    .subscribe(new Observer<TwoFactorResponse>() {
                       @Override
                       public void onCompleted() {
                       }

                       @Override
                       public void onError(Throwable e) {
                       }

                       @Override
                       public void onNext(TwoFactorResponse twoFactorResponse) {
                          //New 2fa code was sent
                       }
                    });
         }
      });

      return root;
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      glideraService = GlideraService.getInstance();
      mbwManager = MbwManager.getInstance(this.getActivity());

      buyMode = getArguments().getString("buyMode");
      btc = getArguments().getString("btc");
      fiat = getArguments().getString("fiat");
      mode2FA = getArguments().getString("mode2FA");
   }
}
