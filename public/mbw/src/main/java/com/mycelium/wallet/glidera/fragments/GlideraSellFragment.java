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
import com.mrd.bitlib.TransactionUtils;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.glidera.GlideraUtils;
import com.mycelium.wallet.glidera.api.GlideraService;
import com.mycelium.wallet.glidera.api.request.SellPriceRequest;
import com.mycelium.wallet.glidera.api.response.GlideraError;
import com.mycelium.wallet.glidera.api.response.SellAddressResponse;
import com.mycelium.wallet.glidera.api.response.SellPriceResponse;
import com.mycelium.wallet.glidera.api.response.TransactionLimitsResponse;

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
    private volatile SellPriceResponse mostRecentSellPriceResponse;
    private volatile TransactionLimitsResponse _transactionLimitsResponse;
    private BigDecimal btcAvailible;

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
                //.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<SellPriceResponse>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        GlideraError error = GlideraService.convertRetrofitException(e);
                        if (error != null && error.getCode() != null) {
                            //TODO handle error
                        }
                    }

                    @Override
                    public void onNext(SellPriceResponse sellPriceResponse) {
                        mostRecentSellPriceResponse = sellPriceResponse;
                        tvSellFiatDescription.setText(sellPriceResponse.getCurrency());
                        tvPrice.setText(GlideraUtils.formatFiatForDisplay(sellPriceResponse.getPrice()));
                        currencyIso = sellPriceResponse.getCurrency();
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
                    etSellFiat.removeTextChangedListener(this);
                    editable.clear();
                    editable.append(value);
                    etSellFiat.addTextChangedListener(this);
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

        etSellFiat.addTextChangedListener(textWatcherFiat);

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
                    etSellBtc.removeTextChangedListener(this);
                    editable.clear();
                    editable.append(value);
                    etSellBtc.addTextChangedListener(this);
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

        etSellBtc.addTextChangedListener(textWatcherBtc);

        buttonSellBitcoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String qty = etSellBtc.getText().toString();

                if (qty.isEmpty()) {
                    etSellBtc.setError("BTC must be greater than 0");
                    return;
                }

                BigDecimal fiat = new BigDecimal(etSellFiat.getText().toString());
                if (fiat.compareTo(_transactionLimitsResponse.getDailySellRemaining()) > 0) {
                    String error = "Amount greater than remaining limit of " + GlideraUtils.formatFiatForDisplay
                            (_transactionLimitsResponse.getDailySellRemaining());
                    etSellFiat.setError(error);
                    return;
                }

                BigDecimal btc = new BigDecimal(etSellBtc.getText().toString());
                if (btcAvailible.compareTo(btc) < 0) {
                    String error = "Insufficient funds";
                    etSellBtc.setError(error);
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
                        DialogFragment newFragment = GlideraSell2faDialog.newInstance(mostRecentSellPriceResponse.getQty(),
                                mostRecentSellPriceResponse.getTotal(), mostRecentSellPriceResponse.getPriceUuid(), sellAddressResponse
                                        .getSellAddress());
                        newFragment.show(getFragmentManager(), "gliderasell2fadialog");
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
        MbwManager mbwManager = MbwManager.getInstance(this.getActivity());
        btcAvailible = mbwManager.getSelectedAccount().calculateMaxSpendableAmount(TransactionUtils.DEFAULT_KB_FEE).getExactValue()
                .getValue();
    }

    private void queryPricing(final BigDecimal btc, final BigDecimal fiat) {
        if (btc != null) {
            if (btc.compareTo(BigDecimal.ZERO) < 0) {
                etSellBtc.setError("BTC must be greater than 0");
                zeroPricing(SellMode.BTC);
                return;
            } else if (btc.compareTo(BigDecimal.ZERO) == 0) {
                zeroPricing(SellMode.BTC);
                return;
            }
        } else if (fiat != null) {
            if (fiat.compareTo(BigDecimal.ZERO) < 0) {
                etSellFiat.setError("BTC must be greater than 0");
                zeroPricing(SellMode.FIAT);
                return;
            } else if (fiat.compareTo(BigDecimal.ZERO) == 0) {
                zeroPricing(SellMode.FIAT);
                return;
            }
        }

        SellPriceRequest sellPriceRequest = new SellPriceRequest(btc, fiat);
        glideraService.sellPrice(sellPriceRequest)
                //.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<SellPriceResponse>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        GlideraError error = GlideraService.convertRetrofitException(e);
                        if (error != null && error.getCode() != null) {
                            if (error.getCode() == 1101) {
                                if (error.getInvalidParameters().contains("fiat")) {
                                    etSellFiat.setError("Invalid " + currencyIso + " value. " + error.getDetails());
                                } else if (error.getInvalidParameters().contains("qty")) {
                                    etSellBtc.setError("Invalid BTC value. " + error.getDetails());
                                }
                            }
                        }
                    }

                    @Override
                    public void onNext(SellPriceResponse sellPriceResponse) {
                        mostRecentSellPriceResponse = sellPriceResponse;

                        SellMode sellMode = null;
                        if (btc != null) {
                            sellMode = SellMode.BTC;
                        } else if (fiat != null) {
                            sellMode = SellMode.FIAT;
                        }

                        updatePricing(sellMode, sellPriceResponse);
                    }
                });

    }

    private void updatePricing(SellMode sellMode, SellPriceResponse sellPriceResponse) {
        if (sellMode == SellMode.BTC) {
            etSellFiat.removeTextChangedListener(textWatcherFiat);
            etSellFiat.setText(sellPriceResponse.getSubtotal().toPlainString());
            etSellFiat.addTextChangedListener(textWatcherFiat);
        } else if (sellMode == SellMode.FIAT) {
            etSellBtc.removeTextChangedListener(textWatcherBtc);
            etSellBtc.setText(sellPriceResponse.getQty().toPlainString());
            etSellBtc.addTextChangedListener(textWatcherBtc);
        } else {
            etSellFiat.removeTextChangedListener(textWatcherFiat);
            etSellBtc.removeTextChangedListener(textWatcherBtc);
            etSellFiat.setText(sellPriceResponse.getSubtotal().toPlainString());
            etSellBtc.setText(sellPriceResponse.getQty().toPlainString());
            etSellFiat.addTextChangedListener(textWatcherFiat);
            etSellBtc.addTextChangedListener(textWatcherBtc);
        }

        tvSubtotal.setText(GlideraUtils.formatFiatForDisplay(sellPriceResponse.getSubtotal()));
        tvBtcAmount.setText(GlideraUtils.formatBtcForDisplay(sellPriceResponse.getQty()));
        tvFeeAmount.setText(GlideraUtils.formatFiatForDisplay(sellPriceResponse.getFees()));
        tvTotalAmount.setText(GlideraUtils.formatFiatForDisplay(sellPriceResponse.getTotal()));
        tvPrice.setText(GlideraUtils.formatFiatForDisplay(sellPriceResponse.getPrice()));

        BigDecimal fiat = new BigDecimal(etSellFiat.getText().toString());
        if (fiat.compareTo(_transactionLimitsResponse.getDailySellRemaining()) > 0) {
            String error = "Amount greater than remaining limit of " + GlideraUtils.formatFiatForDisplay(_transactionLimitsResponse
                    .getDailySellRemaining());

            if (sellMode == SellMode.BTC) {
                etSellBtc.setError(error);
            } else if (sellMode == SellMode.FIAT) {
                etSellFiat.setError(error);
            } else {
                etSellBtc.setError(error);
                etSellFiat.setError(error);
            }
        }

        BigDecimal btc = new BigDecimal(etSellBtc.getText().toString());
        if (btcAvailible.compareTo(btc) < 0) {
            String error = "Insufficient funds";

            if (sellMode == SellMode.BTC) {
                etSellBtc.setError(error);
            } else if (sellMode == SellMode.FIAT) {
                etSellFiat.setError(error);
            } else {
                etSellBtc.setError(error);
                etSellFiat.setError(error);
            }
        }

    }

    private void zeroPricing() {
        zeroPricing(null);
    }

    private void zeroPricing(SellMode sellMode) {
        if (sellMode == SellMode.BTC) {
            etSellFiat.removeTextChangedListener(textWatcherFiat);
            etSellFiat.setText("");
            etSellFiat.addTextChangedListener(textWatcherFiat);
        } else if (sellMode == SellMode.FIAT) {
            etSellBtc.removeTextChangedListener(textWatcherBtc);
            etSellBtc.setText("");
            etSellBtc.addTextChangedListener(textWatcherBtc);
        } else {
            etSellFiat.removeTextChangedListener(textWatcherFiat);
            etSellBtc.removeTextChangedListener(textWatcherBtc);
            etSellFiat.setText("");
            etSellBtc.setText("");
            etSellFiat.addTextChangedListener(textWatcherFiat);
            etSellBtc.addTextChangedListener(textWatcherBtc);
        }

        tvSubtotal.setText(GlideraUtils.formatFiatForDisplay(BigDecimal.ZERO));
        tvBtcAmount.setText(GlideraUtils.formatBtcForDisplay(BigDecimal.ZERO));
        tvFeeAmount.setText(GlideraUtils.formatFiatForDisplay(BigDecimal.ZERO));
        tvTotalAmount.setText(GlideraUtils.formatFiatForDisplay(BigDecimal.ZERO));
    }


    private enum SellMode {
        FIAT, BTC
    }
}
