package com.mycelium.wallet.glidera.fragments;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import com.mycelium.wallet.glidera.api.request.SellPriceRequest;
import com.mycelium.wallet.glidera.api.response.SellPriceResponse;
import com.mycelium.wallet.glidera.api.response.GlideraError;
import com.mycelium.wallet.glidera.api.response.TwoFactorResponse;

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
    private Button buttonSellBitcoin;
    private TextView tvPrice;
    private String currencyIso = "Fiat";
    private Toaster toaster;
    private volatile SellPriceResponse mostRecentSellPriceResponse;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = Preconditions.checkNotNull(inflater.inflate(R.layout.glidera_sell, container, false));

        toaster = new Toaster(getActivity());

        etSellFiat = (EditText) root.findViewById(R.id.etSellFiat);
        etSellBtc = (EditText) root.findViewById(R.id.etSellBtc);
        tvSubtotal = (TextView) root.findViewById(R.id.tvFiatAmount);
        tvBtcAmount = (TextView) root.findViewById(R.id.tvBtcAmount);
        tvFeeAmount = (TextView) root.findViewById(R.id.tvFeeAmount);
        tvTotalAmount = (TextView) root.findViewById(R.id.tvTotalAmount);
        tvPrice = (TextView) root.findViewById(R.id.tvPrice);
        buttonSellBitcoin = (Button) root.findViewById(R.id.buttonSellBitcoin);

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
                            Log.i("Glidera", error.toString());
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
                DialogFragment newFragment = GlideraSell2faDialog.newInstance(mostRecentSellPriceResponse.getQty(),mostRecentSellPriceResponse.getTotal(), TwoFactorResponse.Mode.NONE);
                newFragment.show(getFragmentManager(), "gliderasell2fadialog");
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
                toaster.toast("BTC must be greater than 0", true);
                zeroPricing(SellMode.BTC);
                return;
            } else if (btc.compareTo(BigDecimal.ZERO) == 0) {
                zeroPricing(SellMode.BTC);
                return;
            }
        } else if (fiat != null) {
            if (fiat.compareTo(BigDecimal.ZERO) < 0) {
                toaster.toast("BTC must be greater than 0", true);
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
                                    toaster.toast("Invalid " + currencyIso + " value. " + error.getDetails(), true);
                                } else if (error.getInvalidParameters().contains("qty")) {
                                    toaster.toast("Invalid BTC value. " + error.getDetails(), true);
                                }
                            }
                            Log.i("Glidera", error.toString());
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
