package com.mycelium.wallet.external.glidera.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.TransactionUtils;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.external.glidera.GlideraUtils;
import com.mycelium.wallet.external.glidera.api.GlideraService;
import com.mycelium.wallet.external.glidera.api.request.SellPriceRequest;
import com.mycelium.wallet.external.glidera.api.response.GlideraError;
import com.mycelium.wallet.external.glidera.api.response.SellAddressResponse;
import com.mycelium.wallet.external.glidera.api.response.SellPriceResponse;
import com.mycelium.wallet.external.glidera.api.response.TransactionLimitsResponse;

import java.math.BigDecimal;

import rx.Observer;

public class GlideraSellFragment extends Fragment {

   private GlideraService glideraService;
   private EditText etSellFiat;
   private EditText etSellBtc;
   private TextView tvSubtotal;
   private TextView tvBtcAmount;
   private TextView tvFeeAmount;
   private TextView tvTotalAmount;
   private TextWatcher textWatcherFiat;
   private TextWatcher textWatcherBtc;
   private TextView tvPrice;
   private String currencyIso = "Fiat";
   private TransactionLimitsResponse _transactionLimitsResponse;
   private BigDecimal btcAvailible;
   private volatile SellMode _sellMode;
   private volatile BigDecimal _fiat;
   private volatile BigDecimal _btc;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(false);
      setRetainInstance(true);

      glideraService = GlideraService.getInstance();
      MbwManager mbwManager = MbwManager.getInstance(this.getActivity());
      btcAvailible = mbwManager.getSelectedAccount().calculateMaxSpendableAmount(TransactionUtils.DEFAULT_KB_FEE).getExactValue()
              .getValue();

        /*
        Update prices when fiat is changed
         */
      textWatcherFiat = new TextWatcher() {
         @Override
         public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

         }

         @Override
         public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

         }

         @Override
         public void afterTextChanged(Editable editable) {
            String value = editable.toString();
            int index = editable.toString().indexOf(".");
            int count = index < 0 ? 0 : editable.toString().length() - index - 1;

            if (count > 2) {
               value = value.substring(0, value.length() - count + 2);
               removeTextChangedListeners();
               editable.clear();
               editable.append(value);
               addTextChangedListeners();
            }


            BigDecimal fiat;
            try {
               fiat = new BigDecimal(value);

            } catch (NumberFormatException numberFormatException) {
               fiat = BigDecimal.ZERO;
            }
            queryPricing(null, fiat);
         }
      };

        /*
        Update prices when btc changes
         */
      textWatcherBtc = new TextWatcher() {
         @Override
         public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

         }

         @Override
         public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

         }

         @Override
         public void afterTextChanged(Editable editable) {
            String value = editable.toString();
            int index = editable.toString().indexOf(".");
            int count = index < 0 ? 0 : editable.toString().length() - index - 1;

            if (count > 8) {
               value = value.substring(0, value.length() - count + 8);
               removeTextChangedListeners();
               editable.clear();
               editable.append(value);
               addTextChangedListeners();
            }

            BigDecimal btc;
            try {
               btc = new BigDecimal(value);
            } catch (NumberFormatException numberFormatException) {
               btc = BigDecimal.ZERO;
            }

            queryPricing(btc, null);
         }
      };
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View root = Preconditions.checkNotNull(inflater.inflate(R.layout.glidera_sell, container, false));

      etSellFiat = (EditText) root.findViewById(R.id.etSellFiat);
      etSellBtc = (EditText) root.findViewById(R.id.etSellBtc);
      tvSubtotal = (TextView) root.findViewById(R.id.tvFiatAmount);
      tvBtcAmount = (TextView) root.findViewById(R.id.tvBtcAmount);
      tvFeeAmount = (TextView) root.findViewById(R.id.tvFeeAmount);
      tvTotalAmount = (TextView) root.findViewById(R.id.tvTotalAmount);
      tvPrice = (TextView) root.findViewById(R.id.tvPrice);
      Button buttonSellBitcoin = (Button) root.findViewById(R.id.buttonSellBitcoin);

        /*
        Determine which currency to show
         */
      final TextView tvSellFiatDescription = (TextView) root.findViewById(R.id.tvSellFiatDescription);

      final SellPriceRequest sellPriceRequest = new SellPriceRequest(BigDecimal.ONE, null);

      glideraService.sellPrice(sellPriceRequest)
              .subscribe(new Observer<SellPriceResponse>() {
                 @Override
                 public void onCompleted() {
                 }

                 @Override
                 public void onError(Throwable e) {
                 }

                 @Override
                 public void onNext(SellPriceResponse sellPriceResponse) {
                    tvSellFiatDescription.setText(sellPriceResponse.getCurrency());
                    tvPrice.setText(GlideraUtils.formatFiatForDisplay(sellPriceResponse.getPrice()));
                    currencyIso = sellPriceResponse.getCurrency();
                 }
              });


      glideraService.transactionLimits()
              .subscribe(new Observer<TransactionLimitsResponse>() {
                 @Override
                 public void onCompleted() {
                 }

                 @Override
                 public void onError(Throwable e) {
                 }

                 @Override
                 public void onNext(TransactionLimitsResponse transactionLimitsResponse) {
                    _transactionLimitsResponse = transactionLimitsResponse;
                 }
              });

      buttonSellBitcoin.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {

            String qty = etSellBtc.getText().toString();
            if (qty.isEmpty()) {
               String error = "BTC must be greater than " + GlideraUtils.formatBtcForDisplay(BigDecimal.ZERO);
               setError(SellMode.BTC, error);
               return;
            }

            BigDecimal fiat = new BigDecimal(etSellFiat.getText().toString());
            if (_transactionLimitsResponse == null) {
               String error = "Current transaction limit not available";
               setError(SellMode.FIAT, error);
               return;
            }
            if (fiat.compareTo(_transactionLimitsResponse.getDailySellRemaining()) > 0) {
               String error = "Amount greater than remaining limit of " + GlideraUtils.formatFiatForDisplay
                       (_transactionLimitsResponse.getDailySellRemaining());
               setError(SellMode.FIAT, error);
               return;
            }

            BigDecimal btc = new BigDecimal(etSellBtc.getText().toString());
            if (btcAvailible.compareTo(btc) < 0) {
               String error = "Insufficient funds";
               setError(SellMode.BTC, error);
               return;
            }

            glideraService.sellAddress().subscribe(new Observer<SellAddressResponse>() {
               @Override
               public void onCompleted() {

               }

               @Override
               public void onError(Throwable e) {

               }

               @Override
               public void onNext(SellAddressResponse sellAddressResponse) {
                  DialogFragment newFragment = GlideraSell2faDialog.newInstance(_sellMode, _btc, _fiat, sellAddressResponse
                          .getSellAddress());
                  newFragment.show(getFragmentManager(), "gliderasell2fadialog");
               }
            });
         }
      });

      return root;
   }

   @Override
   public void onResume() {
      super.onResume();

      String value = etSellBtc.getText().toString();
      if (!value.isEmpty()) {
         BigDecimal btc;
         try {
            btc = new BigDecimal(value);
            queryPricing(btc, null);
         } catch (NumberFormatException numberFormatException) {
            //Intentinally empty
         }
      }

      addTextChangedListeners();
   }

   @Override
   public void onStop() {
      super.onStop();
      removeTextChangedListeners();
   }

   private void queryPricing(final BigDecimal btc, final BigDecimal fiat) {
      if (btc != null) {
         if (btc.compareTo(BigDecimal.ZERO) < 0) {
            String error = "BTC must be greater than " + GlideraUtils.formatBtcForDisplay(BigDecimal.ZERO);
            setError(SellMode.BTC, error);
            zeroPricing(SellMode.BTC);
            return;
         } else if (btc.compareTo(BigDecimal.ZERO) == 0) {
            zeroPricing(SellMode.BTC);
            return;
         }
      } else if (fiat != null) {
         if (fiat.compareTo(BigDecimal.ZERO) < 0) {
            String error = currencyIso + " must be greater than " + GlideraUtils.formatFiatForDisplay(BigDecimal.ZERO);
            setError(SellMode.FIAT, error);
            zeroPricing(SellMode.FIAT);
            return;
         } else if (fiat.compareTo(BigDecimal.ZERO) == 0) {
            zeroPricing(SellMode.FIAT);
            return;
         }
      }

      SellPriceRequest sellPriceRequest = new SellPriceRequest(btc, fiat);
      glideraService.sellPrice(sellPriceRequest)
              .subscribe(new Observer<SellPriceResponse>() {
                 @Override
                 public void onCompleted() {
                 }

                 @Override
                 public void onError(Throwable e) {
                    GlideraError error = GlideraService.convertRetrofitException(e);
                    if (error != null && error.getCode() != null) {
                       if (error.getCode() == GlideraError.ERROR_INVALID_VALUE) {
                          if (error.getInvalidParameters().contains("fiat")) {
                             String message = "Invalid " + currencyIso + " value. " + error.getDetails();
                             setError(SellMode.FIAT, message);
                          } else if (error.getInvalidParameters().contains("qty")) {
                             String message = "Invalid BTC value. " + error.getDetails();
                             setError(SellMode.BTC, message);
                          }
                       }
                    }
                 }

                 @Override
                 public void onNext(SellPriceResponse sellPriceResponse) {
                    SellMode sellMode = null;
                    if (btc != null) {
                       sellMode = SellMode.BTC;
                       _sellMode = SellMode.FIAT;
                       _fiat = sellPriceResponse.getSubtotal();
                       _btc = btc;
                    } else if (fiat != null) {
                       sellMode = SellMode.FIAT;
                       _sellMode = SellMode.FIAT;
                       _fiat = fiat;
                       _btc = sellPriceResponse.getQty();
                    }

                    updatePricing(sellMode, sellPriceResponse);
                 }
              });

   }

   private void updatePricing(SellMode sellMode, SellPriceResponse sellPriceResponse) {
      removeTextChangedListeners();
      if (sellMode == SellMode.BTC) {
         etSellFiat.setText(sellPriceResponse.getSubtotal().toPlainString());
      } else if (sellMode == SellMode.FIAT) {
         etSellBtc.setText(sellPriceResponse.getQty().toPlainString());
      } else {
         etSellFiat.setText(sellPriceResponse.getSubtotal().toPlainString());
         etSellBtc.setText(sellPriceResponse.getQty().toPlainString());
      }
      addTextChangedListeners();

      tvSubtotal.setText(GlideraUtils.formatFiatForDisplay(sellPriceResponse.getSubtotal()));
      tvBtcAmount.setText(GlideraUtils.formatBtcForDisplay(sellPriceResponse.getQty()));
      tvFeeAmount.setText(GlideraUtils.formatFiatForDisplay(sellPriceResponse.getFees()));
      tvTotalAmount.setText(GlideraUtils.formatFiatForDisplay(sellPriceResponse.getTotal()));
      tvPrice.setText(GlideraUtils.formatFiatForDisplay(sellPriceResponse.getPrice()));

      BigDecimal fiat = new BigDecimal(etSellFiat.getText().toString());
      if (fiat.compareTo(_transactionLimitsResponse.getDailySellRemaining()) > 0) {
         String error = "Amount greater than remaining limit of " + GlideraUtils.formatFiatForDisplay(_transactionLimitsResponse
                 .getDailySellRemaining());
         setError(sellMode, error);
      }

      BigDecimal btc = new BigDecimal(etSellBtc.getText().toString());
      if (btcAvailible.compareTo(btc) < 0) {
         String error = "Insufficient funds";
         setError(sellMode, error);
      }

   }

   private void zeroPricing(@NonNull SellMode sellMode) {
      removeTextChangedListeners();
      if (sellMode == SellMode.BTC) {
         etSellFiat.setText("");
      } else {
         etSellBtc.setText("");
      }
      addTextChangedListeners();

      tvSubtotal.setText(GlideraUtils.formatFiatForDisplay(BigDecimal.ZERO));
      tvBtcAmount.setText(GlideraUtils.formatBtcForDisplay(BigDecimal.ZERO));
      tvFeeAmount.setText(GlideraUtils.formatFiatForDisplay(BigDecimal.ZERO));
      tvTotalAmount.setText(GlideraUtils.formatFiatForDisplay(BigDecimal.ZERO));
   }

   private void setError(SellMode sellMode, String error) {
      if (sellMode == SellMode.FIAT) {
         etSellBtc.setError(null);
         etSellFiat.setError(error);
      } else {
         etSellFiat.setError(null);
         etSellBtc.setError(error);
      }
   }

   private void addTextChangedListeners() {
      etSellFiat.addTextChangedListener(textWatcherFiat);
      etSellBtc.addTextChangedListener(textWatcherBtc);
   }

   private void removeTextChangedListeners() {
      etSellFiat.removeTextChangedListener(textWatcherFiat);
      etSellBtc.removeTextChangedListener(textWatcherBtc);
   }

   public enum SellMode {
      FIAT, BTC
   }
}
