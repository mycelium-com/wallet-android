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
import com.megiontechnologies.Bitcoins;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.TransactionUtils;
import com.mrd.bitlib.UnsignedTransaction;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.Transaction;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.external.glidera.GlideraUtils;
import com.mycelium.wallet.external.glidera.activities.GlideraTransaction;
import com.mycelium.wallet.external.glidera.api.GlideraService;
import com.mycelium.wallet.external.glidera.api.request.SellPriceRequest;
import com.mycelium.wallet.external.glidera.api.request.SellRequest;
import com.mycelium.wallet.external.glidera.api.response.GlideraError;
import com.mycelium.wallet.external.glidera.api.response.SellPriceResponse;
import com.mycelium.wallet.external.glidera.api.response.SellResponse;
import com.mycelium.wapi.wallet.AddressUtils;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.btc.WalletBtcAccount;

import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import rx.Observer;

public class GlideraSell2faDialog extends DialogFragment {
   private MbwManager mbwManager;
   private GlideraService glideraService;
   private TextView tvPurchaseSummary;
   private ProgressBar pbTimer;

   private String sellMode;
   private String btc;
   private String fiat;
   private String sellAddress;

   private volatile SellPriceResponse _sellPriceResponse;

   static GlideraSell2faDialog newInstance(GlideraSellFragment.SellMode sellMode, BigDecimal btc, BigDecimal fiat, String sellAddress) {
      Bundle bundle = new Bundle();
      bundle.putString("sellMode", sellMode.toString());
      bundle.putString("btc", btc.toPlainString());
      bundle.putString("fiat", fiat.toString());
      bundle.putString("sellAddress", sellAddress);

      GlideraSell2faDialog glideraSell2faDialog = new GlideraSell2faDialog();
      glideraSell2faDialog.setArguments(bundle);

      return glideraSell2faDialog;
   }

   private void updatePrice() {
      SellPriceRequest sellPriceRequest;
      if (sellMode.equals(GlideraBuyFragment.BuyMode.FIAT.toString())) {
         sellPriceRequest = new SellPriceRequest(null, new BigDecimal(fiat));
      } else {
         sellPriceRequest = new SellPriceRequest(new BigDecimal(btc), null);
      }

      glideraService.sellPrice(sellPriceRequest)
              .subscribe(new Observer<SellPriceResponse>() {
                 @Override
                 public void onCompleted() {
                 }

                 @Override
                 public void onError(Throwable e) {
                 }

                 @Override
                 public void onNext(SellPriceResponse sellPriceReesponse) {
                    String purchaseSummary = "You are about to sell " + GlideraUtils.formatBtcForDisplay(sellPriceReesponse.getQty()) +
                            " for " + GlideraUtils.formatFiatForDisplay(sellPriceReesponse.getSubtotal()) + ".";

                    tvPurchaseSummary.setText(purchaseSummary);
                    _sellPriceResponse = sellPriceReesponse;

                    //expire at the expiration time minus 10 seconds to act as a buffer
                    long expiration = sellPriceReesponse.getExpires().getTime() - new Date().getTime() - 10000;

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
      pbTimer = (ProgressBar) root.findViewById(R.id.pbTimer);

      updatePrice();

      TextView tv2FASummary = (TextView) root.findViewById(R.id.tv2FASummary);
      Button buttonResend2FA = (Button) root.findViewById(R.id.buttonResend2FA);
      EditText et2FA = (EditText) root.findViewById(R.id.et2FA);
      final Toaster toaster = new Toaster(getActivity());

      getDialog().setTitle("Confirm Your Purchase");

        /*
        Hide the 2fa stuff on sell
         */
      tv2FASummary.setVisibility(View.GONE);
      buttonResend2FA.setVisibility(View.GONE);
      et2FA.setVisibility(View.GONE);

      Button buttonCancel = (Button) root.findViewById(R.id.buttonCancel);
      buttonCancel.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            GlideraSell2faDialog.this.getDialog().cancel();
         }
      });

      final Button buttonContinue = (Button) root.findViewById(R.id.buttonContinue);
      buttonContinue.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            if (_sellPriceResponse == null) {
               return;
            } else if (_sellPriceResponse.getExpires().before(new Date())) {
               updatePrice();
               return;
            }

            buttonContinue.setEnabled(false);

            Optional<Address> optionalRefundAddress = ((WalletBtcAccount)(mbwManager.getSelectedAccount())).getReceivingAddress();

            if (optionalRefundAddress.isPresent()) {
               Address refundAddress = optionalRefundAddress.get();
               UUID uuid = _sellPriceResponse.getPriceUuid();

               List<WalletAccount.Receiver> receivers = new ArrayList<>();
               Address address = Address.fromString(sellAddress);
               receivers.add(new WalletAccount.Receiver(AddressUtils.fromAddress(address), Bitcoins.nearestValue
                          (_sellPriceResponse.getQty())));

               WalletAccount selectedAccount = mbwManager.getSelectedAccount();
               final UnsignedTransaction unsignedTransaction;

               try {
                  unsignedTransaction = ((WalletBtcAccount)selectedAccount).createUnsignedTransaction(receivers, TransactionUtils.DEFAULT_KB_FEE);
               } catch (StandardTransactionBuilder.OutputTooSmallException outputTooSmallException) {
                  outputTooSmallException.printStackTrace();
                  buttonContinue.setEnabled(false);
                  toaster.toast("Amount too small", true);
                  return;
               } catch (StandardTransactionBuilder.InsufficientFundsException insufficientFundsException) {
                  insufficientFundsException.printStackTrace();
                  buttonContinue.setEnabled(false);
                  toaster.toast("Insufficient funds", true);
                  return;
               } catch (StandardTransactionBuilder.UnableToBuildTransactionException unableToBuildTransactionException) {
                  unableToBuildTransactionException.printStackTrace();
                  buttonContinue.setEnabled(false);
                  toaster.toast("An error occured", true);
                  return;
               }

               final Transaction signedTransaction;
               try {
                  signedTransaction = ((WalletBtcAccount)selectedAccount).signTransaction(unsignedTransaction, AesKeyCipher.defaultKeyCipher());
               } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
                  invalidKeyCipher.printStackTrace();
                  buttonContinue.setEnabled(false);
                  toaster.toast("An error occured", true);
                  return;
               }

               final String rawTransaction;
               ByteArrayOutputStream stream = new ByteArrayOutputStream();
               try {
                  Hex.encode(signedTransaction.toBytes(), stream);
                  rawTransaction = stream.toString();
               } catch (IOException ioException) {
                  ioException.printStackTrace();
                  buttonContinue.setEnabled(false);
                  toaster.toast("An error occured", true);
                  return;
               }

               SellRequest sellRequest = new SellRequest(refundAddress, rawTransaction, uuid, false, null);

               glideraService.sell(sellRequest).subscribe(new Observer<SellResponse>() {
                  @Override
                  public void onCompleted() {
                  }

                  @Override
                  public void onError(Throwable e) {
                     GlideraError error = GlideraService.convertRetrofitException(e);
                     if (error != null && error.getCode() != null) {
                        switch (error.getCode()) {
                           case GlideraError.ERROR_TRANSACTION_FAILED_COINS_RETURNED:
                              toaster.toast("An error has occured and your coin returned.", true);
                              break;
                           case GlideraError.ERROR_OCCURRED_CALL_SUPPORT:
                              toaster.toast("An error has occured, please contact Glidera support", true);
                              break;
                           default:
                              toaster.toast("Unable to sell at this time", true);
                              break;
                        }
                     }
                     buttonContinue.setEnabled(true);
                  }

                  @Override
                  public void onNext(SellResponse sellResponse) {
                     Intent intent = new Intent(getActivity(), GlideraTransaction.class);
                     Bundle bundle = new Bundle();
                     bundle.putString("transactionuuid", sellResponse.getTransactionUuid().toString());
                     intent.putExtras(bundle);
                     startActivity(intent);
                  }
               });
            }
         }
      });

      return root;
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      glideraService = GlideraService.getInstance();
      mbwManager = MbwManager.getInstance(this.getActivity());

      sellMode = getArguments().getString("sellMode");
      btc = getArguments().getString("btc");
      fiat = getArguments().getString("fiat");
      sellAddress = getArguments().getString("sellAddress");
   }
}
