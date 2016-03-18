package com.mycelium.wallet.glidera.fragments;

import android.os.Bundle;
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
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.glidera.GlideraUtils;
import com.mycelium.wallet.glidera.api.GlideraService;
import com.mycelium.wallet.glidera.api.request.BuyPriceRequest;
import com.mycelium.wallet.glidera.api.response.BuyPriceResponse;
import com.mycelium.wallet.glidera.api.response.GlideraError;
import com.mycelium.wallet.glidera.api.response.TransactionLimitsResponse;
import com.mycelium.wallet.glidera.api.response.TwoFactorResponse;

import java.math.BigDecimal;

import rx.Observer;

public class GlideraBuyFragment extends Fragment {
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
    private volatile BuyPriceResponse mostRecentBuyPriceResponse;
    private volatile TransactionLimitsResponse _transactionLimitsResponse;

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
        Button buttonBuyBitcoin = (Button) root.findViewById(R.id.buttonBuyBitcoin);

        /*
        Determine which currency to show
         */
        final TextView tvBuyFiatDescription = (TextView) root.findViewById(R.id.tvBuyFiatDescription);

        final BuyPriceRequest buyPriceRequest = new BuyPriceRequest(BigDecimal.ONE, null);

        glideraService.buyPrice(buyPriceRequest)
                //.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<BuyPriceResponse>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(BuyPriceResponse buyPriceResponse) {
                        mostRecentBuyPriceResponse = buyPriceResponse;
                        tvBuyFiatDescription.setText(buyPriceResponse.getCurrency());
                        tvPrice.setText(GlideraUtils.formatFiatForDisplay(buyPriceResponse.getPrice()));
                        currencyIso = buyPriceResponse.getCurrency();
                        zeroPricing();
                    }
                });


        glideraService.transactionLimits()
                //.observeOn(AndroidSchedulers.mainThread())
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
                    etBuyFiat.removeTextChangedListener(this);
                    editable.clear();
                    editable.append(value);
                    etBuyFiat.addTextChangedListener(this);
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

        etBuyFiat.addTextChangedListener(textWatcherFiat);

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
                    etBuyBtc.removeTextChangedListener(this);
                    editable.clear();
                    editable.append(value);
                    etBuyBtc.addTextChangedListener(this);
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

        etBuyBtc.addTextChangedListener(textWatcherBtc);

        buttonBuyBitcoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String qty = etBuyBtc.getText().toString();

                if( qty == null || qty.isEmpty() ) {
                    etBuyBtc.setError("BTC must be greater than 0");
                    return;
                }

                BigDecimal fiat = new BigDecimal(etBuyFiat.getText().toString());
                if( fiat.compareTo(_transactionLimitsResponse.getDailyBuyRemaining()) > 0 ) {
                    String error = "Amount greater than remaining limit of " + GlideraUtils.formatFiatForDisplay(_transactionLimitsResponse.getDailyBuyRemaining());
                    etBuyFiat.setError(error);
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
                                DialogFragment newFragment = GlideraBuy2faDialog.newInstance(mostRecentBuyPriceResponse.getQty(),
                                        mostRecentBuyPriceResponse.getTotal(), twoFactorResponse.getMode(), mostRecentBuyPriceResponse.getPriceUuid());
                                newFragment.show(getFragmentManager(), "gliderabuy2fadialog");
                            }
                        });
            }
        });

        return root;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);

        glideraService = GlideraService.getInstance();
    }

    private void queryPricing(final BigDecimal btc, final BigDecimal fiat) {
        if (btc != null) {
            if (btc.compareTo(BigDecimal.ZERO) < 0) {
                etBuyBtc.setError("BTC must be greater than 0");
                zeroPricing(BuyMode.BTC);
                return;
            } else if (btc.compareTo(BigDecimal.ZERO) == 0) {
                zeroPricing(BuyMode.BTC);
                return;
            }
        } else if (fiat != null) {
            if (fiat.compareTo(BigDecimal.ZERO) < 0) {
                etBuyFiat.setError("BTC must be greater than 0");
                zeroPricing(BuyMode.FIAT);
                return;
            } else if (fiat.compareTo(BigDecimal.ZERO) == 0) {
                zeroPricing(BuyMode.FIAT);
                return;
            }
        }

        BuyPriceRequest buyPriceRequest = new BuyPriceRequest(btc, fiat);
        glideraService.buyPrice(buyPriceRequest)
                //.observeOn(AndroidSchedulers.mainThread())
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
                                    etBuyFiat.setError("Invalid " + currencyIso + " value. " + error.getDetails());
                                } else if (error.getInvalidParameters().contains("qty")) {
                                    etBuyBtc.setError("Invalid BTC value. " + error.getDetails());
                                }
                            }
                        }
                    }

                    @Override
                    public void onNext(BuyPriceResponse buyPriceResponse) {
                        mostRecentBuyPriceResponse = buyPriceResponse;

                        BuyMode buyMode = null;
                        if (btc != null) {
                            buyMode = BuyMode.BTC;
                        } else if (fiat != null) {
                            buyMode = BuyMode.FIAT;
                        }

                        updatePricing(buyMode, buyPriceResponse);
                    }
                });

    }

    private void updatePricing(BuyMode buyMode, BuyPriceResponse buyPriceResponse) {
        if (buyMode == BuyMode.BTC) {
            etBuyFiat.removeTextChangedListener(textWatcherFiat);
            etBuyFiat.setText(buyPriceResponse.getSubtotal().toPlainString());
            etBuyFiat.addTextChangedListener(textWatcherFiat);
        } else if (buyMode == BuyMode.FIAT) {
            etBuyBtc.removeTextChangedListener(textWatcherBtc);
            etBuyBtc.setText(buyPriceResponse.getQty().toPlainString());
            etBuyBtc.addTextChangedListener(textWatcherBtc);
        } else {
            etBuyFiat.removeTextChangedListener(textWatcherFiat);
            etBuyBtc.removeTextChangedListener(textWatcherBtc);
            etBuyFiat.setText(buyPriceResponse.getSubtotal().toPlainString());
            etBuyBtc.setText(buyPriceResponse.getQty().toPlainString());
            etBuyFiat.addTextChangedListener(textWatcherFiat);
            etBuyBtc.addTextChangedListener(textWatcherBtc);
        }

        tvSubtotal.setText(GlideraUtils.formatFiatForDisplay(buyPriceResponse.getSubtotal()));
        tvBtcAmount.setText(GlideraUtils.formatBtcForDisplay(buyPriceResponse.getQty()));
        tvFeeAmount.setText(GlideraUtils.formatFiatForDisplay(buyPriceResponse.getFees()));
        tvTotalAmount.setText(GlideraUtils.formatFiatForDisplay(buyPriceResponse.getTotal()));
        tvPrice.setText(GlideraUtils.formatFiatForDisplay(buyPriceResponse.getPrice()));

        BigDecimal fiat = new BigDecimal(etBuyFiat.getText().toString());
        if( fiat.compareTo(_transactionLimitsResponse.getDailyBuyRemaining()) > 0 ) {
            String error = "Amount greater than remaining limit of " + GlideraUtils.formatFiatForDisplay(_transactionLimitsResponse.getDailyBuyRemaining());
            etBuyFiat.setError(error);
            return;
        }
    }

    private void zeroPricing() {
        zeroPricing(null);
    }

    private void zeroPricing(BuyMode buyMode) {
        if (buyMode == BuyMode.BTC) {
            etBuyFiat.removeTextChangedListener(textWatcherFiat);
            etBuyFiat.setText("");
            etBuyFiat.addTextChangedListener(textWatcherFiat);
        } else if (buyMode == BuyMode.FIAT) {
            etBuyBtc.removeTextChangedListener(textWatcherBtc);
            etBuyBtc.setText("");
            etBuyBtc.addTextChangedListener(textWatcherBtc);
        } else {
            etBuyFiat.removeTextChangedListener(textWatcherFiat);
            etBuyBtc.removeTextChangedListener(textWatcherBtc);
            etBuyFiat.setText("");
            etBuyBtc.setText("");
            etBuyFiat.addTextChangedListener(textWatcherFiat);
            etBuyBtc.addTextChangedListener(textWatcherBtc);
        }

        tvSubtotal.setText(GlideraUtils.formatFiatForDisplay(BigDecimal.ZERO));
        tvBtcAmount.setText(GlideraUtils.formatBtcForDisplay(BigDecimal.ZERO));
        tvFeeAmount.setText(GlideraUtils.formatFiatForDisplay(BigDecimal.ZERO));
        tvTotalAmount.setText(GlideraUtils.formatFiatForDisplay(BigDecimal.ZERO));
    }


    private enum BuyMode {
        FIAT, BTC
    }
}
