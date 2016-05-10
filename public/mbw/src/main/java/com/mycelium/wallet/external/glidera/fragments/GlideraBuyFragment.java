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
import com.mycelium.wallet.R;
import com.mycelium.wallet.external.glidera.GlideraUtils;
import com.mycelium.wallet.external.glidera.api.GlideraService;
import com.mycelium.wallet.external.glidera.api.request.BuyPriceRequest;
import com.mycelium.wallet.external.glidera.api.response.BuyPriceResponse;
import com.mycelium.wallet.external.glidera.api.response.GlideraError;
import com.mycelium.wallet.external.glidera.api.response.TransactionLimitsResponse;
import com.mycelium.wallet.external.glidera.api.response.TwoFactorResponse;

import java.math.BigDecimal;

import rx.Observer;

public class GlideraBuyFragment extends Fragment {
    public enum BuyMode {
        FIAT, BTC
    }

    private GlideraService glideraService;
    private EditText etBuyFiat;
    private EditText etBuyBtc;
    private TextView tvSubtotal;
    private TextView tvBtcAmount;
    private TextView tvFeeAmount;
    private TextView tvTotalAmount;
    private TextWatcher textWatcherFiat;
    private TextWatcher textWatcherBtc;
    private TextView tvPrice;
    private String currencyIso = "Fiat";
    private TransactionLimitsResponse _transactionLimitsResponse;
    private volatile BuyMode _buyMode;
    private volatile BigDecimal _fiat;
    private volatile BigDecimal _btc;
    private TextView tvBuyFiatDescription;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        setRetainInstance(true);

        glideraService = GlideraService.getInstance();

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
        View root = Preconditions.checkNotNull(inflater.inflate(R.layout.glidera_buy, container, false));

        etBuyFiat = (EditText) root.findViewById(R.id.etBuyFiat);
        etBuyBtc = (EditText) root.findViewById(R.id.etBuyBtc);
        tvSubtotal = (TextView) root.findViewById(R.id.tvFiatAmount);
        tvBtcAmount = (TextView) root.findViewById(R.id.tvBtcAmount);
        tvFeeAmount = (TextView) root.findViewById(R.id.tvFeeAmount);
        tvTotalAmount = (TextView) root.findViewById(R.id.tvTotalAmount);
        tvPrice = (TextView) root.findViewById(R.id.tvPrice);
        tvBuyFiatDescription = (TextView) root.findViewById(R.id.tvBuyFiatDescription);

        Button buttonBuyBitcoin = (Button) root.findViewById(R.id.buttonBuyBitcoin);

        /*
        Determine which currency to show
         */

        final BuyPriceRequest buyPriceRequest = new BuyPriceRequest(BigDecimal.ONE, null);
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
                        tvBuyFiatDescription.setText(buyPriceResponse.getCurrency());
                        tvPrice.setText(GlideraUtils.formatFiatForDisplay(buyPriceResponse.getPrice()));
                        currencyIso = buyPriceResponse.getCurrency();
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

        buttonBuyBitcoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String qty = etBuyBtc.getText().toString();
                if (qty.isEmpty()) {
                    String error = "BTC must be greater than " + GlideraUtils.formatBtcForDisplay(BigDecimal.ZERO);
                    setError(BuyMode.BTC, error);
                    return;
                }

                BigDecimal fiat = new BigDecimal(etBuyFiat.getText().toString());
                if (fiat.compareTo(_transactionLimitsResponse.getDailyBuyRemaining()) > 0) {
                    String error = "Amount greater than remaining limit of " + GlideraUtils.formatFiatForDisplay
                            (_transactionLimitsResponse.getDailyBuyRemaining());
                    setError(BuyMode.FIAT, error);
                    return;
                }

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
                                DialogFragment newFragment = GlideraBuy2faDialog.newInstance(_buyMode, _btc, _fiat, twoFactorResponse.getMode());
                                newFragment.show(getFragmentManager(), "gliderabuy2fadialog");
                            }
                        });
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        String value = etBuyBtc.getText().toString();
        if( !value.isEmpty() ) {
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
                setError(BuyMode.BTC, error);
                zeroPricing(BuyMode.BTC);
                return;
            } else if (btc.compareTo(BigDecimal.ZERO) == 0) {
                zeroPricing(BuyMode.BTC);
                return;
            }
        } else if (fiat != null) {
            if (fiat.compareTo(BigDecimal.ZERO) < 0) {
                String error = currencyIso + " must be greater than " + GlideraUtils.formatFiatForDisplay(BigDecimal.ZERO);
                setError(BuyMode.FIAT, error);
                zeroPricing(BuyMode.FIAT);
                return;
            } else if (fiat.compareTo(BigDecimal.ZERO) == 0) {
                zeroPricing(BuyMode.FIAT);
                return;
            }
        }

        BuyPriceRequest buyPriceRequest = new BuyPriceRequest(btc, fiat);
        glideraService.buyPrice(buyPriceRequest)
                .subscribe(new Observer<BuyPriceResponse>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        GlideraError error = GlideraService.convertRetrofitException(e);
                        if (error != null && error.getCode() != null) {
                            if (error.getCode() == 1101) {
                                if (error.getInvalidParameters().contains("fiat")) {
                                    String message = "Invalid " + currencyIso + " value. " + error.getDetails();
                                    setError(BuyMode.FIAT, message);
                                } else if (error.getInvalidParameters().contains("qty")) {
                                    String message = "Invalid BTC value. " + error.getDetails();
                                    setError(BuyMode.BTC, message);
                                }
                            }
                        }
                    }

                    @Override
                    public void onNext(BuyPriceResponse buyPriceResponse) {
                        BuyMode buyMode = null;
                        if (btc != null) {
                            buyMode = BuyMode.BTC;
                            _buyMode = BuyMode.BTC;
                            _btc = btc;
                            _fiat = buyPriceResponse.getSubtotal();
                        } else if (fiat != null) {
                            buyMode = BuyMode.FIAT;
                            _buyMode = BuyMode.FIAT;
                            _fiat = fiat;
                            _btc = buyPriceResponse.getQty();
                        }

                        updatePricing(buyMode, buyPriceResponse);
                    }
                });

    }

    private void updatePricing(BuyMode buyMode, BuyPriceResponse buyPriceResponse) {
        removeTextChangedListeners();
        if (buyMode == BuyMode.BTC) {
            etBuyFiat.setText(buyPriceResponse.getSubtotal().toPlainString());
        } else if (buyMode == BuyMode.FIAT) {
            etBuyBtc.setText(buyPriceResponse.getQty().toPlainString());
        } else {
            etBuyFiat.setText(buyPriceResponse.getSubtotal().toPlainString());
            etBuyBtc.setText(buyPriceResponse.getQty().toPlainString());
        }
        addTextChangedListeners();

        tvSubtotal.setText(GlideraUtils.formatFiatForDisplay(buyPriceResponse.getSubtotal()));
        tvBtcAmount.setText(GlideraUtils.formatBtcForDisplay(buyPriceResponse.getQty()));
        tvFeeAmount.setText(GlideraUtils.formatFiatForDisplay(buyPriceResponse.getFees()));
        tvTotalAmount.setText(GlideraUtils.formatFiatForDisplay(buyPriceResponse.getTotal()));
        tvPrice.setText(GlideraUtils.formatFiatForDisplay(buyPriceResponse.getPrice()));

        BigDecimal fiat = new BigDecimal(etBuyFiat.getText().toString());
        if (fiat.compareTo(_transactionLimitsResponse.getDailyBuyRemaining()) > 0) {
            String error = "Amount greater than remaining limit of " + GlideraUtils.formatFiatForDisplay(_transactionLimitsResponse
                    .getDailyBuyRemaining());

            setError(buyMode, error);
        }
    }

    private void zeroPricing(@NonNull BuyMode buyMode) {
        removeTextChangedListeners();
        if (buyMode == BuyMode.BTC) {
            etBuyFiat.setText("");
        } else if (buyMode == BuyMode.FIAT) {
            etBuyBtc.setText("");
        } else {
            etBuyFiat.setText("");
            etBuyBtc.setText("");
        }
        addTextChangedListeners();

        tvSubtotal.setText(GlideraUtils.formatFiatForDisplay(BigDecimal.ZERO));
        tvBtcAmount.setText(GlideraUtils.formatBtcForDisplay(BigDecimal.ZERO));
        tvFeeAmount.setText(GlideraUtils.formatFiatForDisplay(BigDecimal.ZERO));
        tvTotalAmount.setText(GlideraUtils.formatFiatForDisplay(BigDecimal.ZERO));
    }

    private void setError(BuyMode buyMode, String error) {
        if (buyMode == BuyMode.FIAT) {
            etBuyBtc.setError(null);
            etBuyFiat.setError(error);
        } else {
            etBuyFiat.setError(null);
            etBuyBtc.setError(error);
        }
    }

    private void addTextChangedListeners() {
        etBuyFiat.addTextChangedListener(textWatcherFiat);
        etBuyBtc.addTextChangedListener(textWatcherBtc);
    }

    private void removeTextChangedListeners() {
        etBuyFiat.removeTextChangedListener(textWatcherFiat);
        etBuyBtc.removeTextChangedListener(textWatcherBtc);
    }
}
