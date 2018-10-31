package com.mycelium.wallet.external.changelly.bch;


import android.app.DownloadManager;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.megiontechnologies.BitcoinCash;
import com.mycelium.spvmodule.IntentContract;
import com.mycelium.spvmodule.TransactionFee;
import com.mycelium.wallet.BuildConfig;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.WalletApplication;
import com.mycelium.wallet.event.SpvSendFundsResult;
import com.mycelium.wallet.external.changelly.ChangellyAPIService;
import com.mycelium.wallet.external.changelly.ChangellyAPIService.ChangellyTransaction;
import com.mycelium.wallet.external.changelly.Constants;
import com.mycelium.wallet.external.changelly.ExchangeLoggingService;
import com.mycelium.wallet.external.changelly.model.Order;
import com.mycelium.wallet.pdf.BCHExchangeReceiptBuilder;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.bip44.Bip44BCHAccount;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinCashValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinValue;
import com.mycelium.wapi.wallet.single.SingleAddressBCHAccount;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit.RetrofitError;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.DOWNLOAD_SERVICE;
import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static com.mycelium.wallet.external.changelly.ChangellyAPIService.BCH;
import static com.mycelium.wallet.external.changelly.ChangellyAPIService.BTC;
import static com.mycelium.wallet.external.changelly.Constants.ABOUT;
import static com.mycelium.wallet.external.changelly.Constants.decimalFormat;
import static com.mycelium.wallet.external.changelly.bch.ExchangeFragment.BCH_EXCHANGE;
import static com.mycelium.wallet.external.changelly.bch.ExchangeFragment.BCH_EXCHANGE_TRANSACTIONS;
import static com.mycelium.wapi.wallet.bip44.HDAccountContext.ACCOUNT_TYPE_FROM_MASTERSEED;

public class ConfirmExchangeFragment extends Fragment {
    public static final String TAG = "BCHExchange";
    public static final int UPDATE_TIME = 60;
    public static final String BLOCKTRAIL_TRANSACTION = "https://www.blocktrail.com/_network_/tx/_id_";
    private ChangellyAPIService changellyAPIService = ChangellyAPIService.retrofit.create(ChangellyAPIService.class);

    @BindView(R.id.fromAddress)
    TextView fromAddress;

    @BindView(R.id.toAddress)
    TextView toAddress;

    @BindView(R.id.fromLabel)
    TextView fromLabel;

    @BindView(R.id.toLabel)
    TextView toLabel;

    @BindView(R.id.fromAmount)
    TextView fromAmount;

    @BindView(R.id.toAmount)
    TextView toAmount;

    @BindView(R.id.toFiat)
    TextView toFiat;

    @BindView(R.id.buttonContinue)
    Button buttonContinue;

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    @BindView(R.id.offer_update_text)
    TextView offerUpdateText;

    MbwManager mbwManager;
    WalletAccount fromAccount;
    WalletAccount toAccount;
    Double amount;
    Double sentAmount;

    private ChangellyAPIService.ChangellyTransactionOffer offer;
    private ProgressDialog progressDialog;

    private String lastOperationId;
    private String toValue;

    private AlertDialog downloadConfirmationDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        UUID toAddress = (UUID) getArguments().getSerializable(Constants.DESTADDRESS);
        UUID fromAddress = (UUID) getArguments().getSerializable(Constants.FROM_ADDRESS);
        toValue = getArguments().getString(Constants.TO_AMOUNT);
        amount = getArguments().getDouble(Constants.FROM_AMOUNT);
        mbwManager = MbwManager.getInstance(getActivity());
        mbwManager.getEventBus().register(this);
        fromAccount = mbwManager.getWalletManager(false).getAccount(fromAddress);
        toAccount = mbwManager.getWalletManager(false).getAccount(toAddress);
        BigDecimal txFee = UtilsKt.estimateFeeFromTransferrableAmount(
                fromAccount, mbwManager, BitcoinCash.valueOf(amount).getLongValue());
        sentAmount = amount - txFee.doubleValue();
        createOffer();
    }

    @OnClick(R.id.buttonContinue)
    void createAndSignTransaction() {
        mbwManager.runPinProtectedFunction(getActivity(), new Runnable() {
            @Override
            public void run() {
                buttonContinue.setEnabled(false);
                long fromValue = ExactBitcoinCashValue.from(BigDecimal.valueOf(sentAmount)).getLongValue();

                lastOperationId = UUID.randomUUID().toString();

                String payAddress = null;
                try {
                    payAddress = BCHBechAddress.bchBechDecode(offer.payinAddress).constructLegacyAddress(mbwManager.getNetwork()).toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Intent service;
                switch (fromAccount.getType()) {
                    case BCHBIP44: {
                        Bip44BCHAccount bip44BCHAccount = (Bip44BCHAccount) fromAccount;
                        if (bip44BCHAccount.getAccountType() == ACCOUNT_TYPE_FROM_MASTERSEED) {
                            service = IntentContract.SendFunds.createIntent(lastOperationId, bip44BCHAccount.getAccountIndex(),
                                    payAddress, fromValue, TransactionFee.NORMAL, 1.0f);
                        } else {
                            service = IntentContract.SendFundsUnrelated.createIntent(lastOperationId, bip44BCHAccount.getId().toString(), payAddress, fromValue, TransactionFee.NORMAL, 1.0f, IntentContract.UNRELATED_ACCOUNT_TYPE_HD);
                        }
                        WalletApplication.sendToSpv(service, WalletAccount.Type.BCHBIP44);
                        break;
                    }
                    case BCHSINGLEADDRESS: {
                        SingleAddressBCHAccount singleAddressAccount = (SingleAddressBCHAccount) fromAccount;
                        service = IntentContract.SendFundsUnrelated.createIntent(lastOperationId, singleAddressAccount.getId().toString(), payAddress, fromValue, TransactionFee.NORMAL, 1.0f, IntentContract.UNRELATED_ACCOUNT_TYPE_SA);
                        WalletApplication.sendToSpv(service, WalletAccount.Type.BCHSINGLEADDRESS);
                        break;
                    }
                }
                progressDialog = new ProgressDialog(getActivity());
                progressDialog.setIndeterminate(true);
                progressDialog.setMessage(getString(R.string.sending, "..."));
                progressDialog.show();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exchage_confirm, container, false);
        ButterKnife.bind(this, view);
        updateUI();
        if (offer == null) {
            progressBar.setVisibility(View.VISIBLE);
            offerUpdateText.setText(R.string.updating_offer);
            buttonContinue.setEnabled(false);
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fromAddress.setText(fromAccount.getReceivingAddress().get().getShortAddress());
        toAddress.setText(toAccount.getReceivingAddress().get().getShortAddress());

        fromLabel.setText(mbwManager.getMetadataStorage().getLabelByAccount(fromAccount.getId()));
        toLabel.setText(mbwManager.getMetadataStorage().getLabelByAccount(toAccount.getId()));
    }

    @Override
    public void onResume() {
        super.onResume();
        getRate();
    }

    private void updateRate() {
        if (offer != null) {
            CurrencyValue currencyValueTo = null;
            try {
                currencyValueTo = mbwManager.getCurrencySwitcher().getAsFiatValue(
                        ExactBitcoinValue.from(new BigDecimal(toValue)));
            } catch (NumberFormatException ignore) {
            }
            if (currencyValueTo != null && currencyValueTo.getValue() != null) {
                toFiat.setText(ABOUT + Utils.formatFiatWithUnit(currencyValueTo));
                toFiat.setVisibility(View.VISIBLE);
            } else {
                toFiat.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void createOffer() {
        changellyAPIService.createTransaction(BCH, BTC, sentAmount, toAccount.getReceivingAddress().get().toString())
                .enqueue(new GetOfferCallback());
    }

    private void getRate() {
        changellyAPIService.getExchangeAmount(BCH, BTC, sentAmount).enqueue(new GetAmountCallback(sentAmount));
    }

    private void updateUI() {
        if (isAdded()) {
            fromAmount.setText(getString(R.string.value_currency, decimalFormat.format(amount), BCH));
            toAmount.setText(getString(R.string.value_currency, toValue, BTC));
            updateRate();
        }
    }

    int autoUpdateTime;
    private Runnable updateOffer = new Runnable() {
        @Override
        public void run() {
            if (!isAdded()) {
                return;
            }
            autoUpdateTime++;
            offerUpdateText.setText(getString(R.string.offer_auto_updated, UPDATE_TIME - autoUpdateTime));
            if (autoUpdateTime < UPDATE_TIME) {
                offerUpdateText.postDelayed(this, TimeUnit.SECONDS.toMillis(1));
            } else {
                getRate();
            }
        }
    };

    @Subscribe
    public void spvSendFundsResult(SpvSendFundsResult event) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if (!event.operationId.equals(lastOperationId)) {
            return;
        }

        if (!event.isSuccess) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(Html.fromHtml("<big>" + getString(R.string.error) + "</big>"))
                    .setMessage("Send funds failed: " + event.message)
                    .setNegativeButton(R.string.close, null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            getActivity().finish();
                        }
                    })
                    .create().show();
            return;
        }
        final Order order = new Order();
        order.transactionId = event.txHash;
        order.order_id = offer.id;
        order.exchangingAmount = decimalFormat.format(amount);
        order.exchangingCurrency = CurrencyValue.BCH;

        order.receivingAddress = toAccount.getReceivingAddress().get().toString();
        order.receivingAmount = toValue;
        order.receivingCurrency = CurrencyValue.BTC;
        order.timestamp = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.LONG, Locale.ENGLISH)
                .format(new Date());

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_exchange_download_confirmation, null);
        ((TextView) view.findViewById(R.id.title)).setText(R.string.success);
        ((TextView) view.findViewById(R.id.date_time)).setText(getString(R.string.exchange_order_date, order.timestamp));
        ((TextView) view.findViewById(R.id.exchanging)).setText(getString(R.string.exchange_order_exchanging, order.exchangingAmount));
        ((TextView) view.findViewById(R.id.receiving)).setText(getString(R.string.exchange_order_receiving, order.receivingAmount));
        TextView transactionId = view.findViewById(R.id.transaction_id);
        transactionId.setPaintFlags(transactionId.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        transactionId.setText(order.transactionId);
        transactionId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String uri = BLOCKTRAIL_TRANSACTION
                            .replaceAll("_network_", (BuildConfig.FLAVOR.equals("btctestnet") ? "tBCC" : "BCC"))
                            .replaceAll("_id_", order.transactionId);
                    startActivity(Intent.parseUri(uri, Intent.URI_INTENT_SCHEME));
                } catch (URISyntaxException e) {
                    Log.e(TAG, "look transaction on blocktrail ", e);
                }
            }
        });
        view.findViewById(R.id.download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String filePart = new SimpleDateFormat("yyMMddHHmmss", Locale.US).format(new Date());
                File pdfFile = new File(getActivity().getExternalFilesDir(DIRECTORY_DOWNLOADS), "exchange_bch_order_" + filePart + ".pdf");
                try {
                    try (OutputStream pdfStream = new FileOutputStream(pdfFile)) {
                        new BCHExchangeReceiptBuilder()
                                .setTransactionId(order.transactionId)
                                .setDate(order.timestamp)
                                .setReceivingAmount(order.receivingAmount + " " + order.receivingCurrency)
                                .setReceivingAddress(order.receivingAddress)
                                .setReceivingAccountLabel(mbwManager.getMetadataStorage().getLabelByAccount(toAccount.getId()))
                                .setSpendingAmount(order.exchangingAmount + " " + order.exchangingCurrency)
                                .setSpendingAccountLabel(mbwManager.getMetadataStorage().getLabelByAccount(fromAccount.getId()))
                                .build(pdfStream);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "", e);
                }
                DownloadManager downloadManager = (DownloadManager) getActivity().getSystemService(DOWNLOAD_SERVICE);
                downloadManager.addCompletedDownload(pdfFile.getName(), pdfFile.getName()
                        , true, "application/pdf"
                        , pdfFile.getAbsolutePath(), pdfFile.length(), true);
                downloadConfirmationDialog.dismiss();
            }
        });
        view.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadConfirmationDialog.dismiss();
            }
        });
        downloadConfirmationDialog = new AlertDialog.Builder(getActivity(), R.style.MyceliumModern_Dialog)

                .setView(view)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        getActivity().finish();
                    }
                })
                .create();
        downloadConfirmationDialog.show();

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(BCH_EXCHANGE, Context.MODE_PRIVATE);
        Set<String> exchangeTransactions = sharedPreferences.getStringSet(BCH_EXCHANGE_TRANSACTIONS, new HashSet<String>());
        exchangeTransactions.add(order.transactionId);
        sharedPreferences.edit()
            .putStringSet(BCH_EXCHANGE_TRANSACTIONS, exchangeTransactions).apply();

        try {
            ExchangeLoggingService.exchangeLoggingService.saveOrder(order).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    Log.d(TAG, "logging success ");
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.d(TAG, "logging failure", t);
                }
            });
        } catch (RetrofitError e) {
            Log.e(TAG, "Excange logging error", e);
        }
    }

    class GetAmountCallback implements Callback<ChangellyAPIService.ChangellyAnswerDouble> {
        double fromAmount;

        GetAmountCallback(double fromAmount) {
            this.fromAmount = fromAmount;
        }

        @Override
        public void onResponse(@NonNull Call<ChangellyAPIService.ChangellyAnswerDouble> call,
                               @NonNull Response<ChangellyAPIService.ChangellyAnswerDouble> response) {
            ChangellyAPIService.ChangellyAnswerDouble result = response.body();
            if(result != null) {
                double amount = result.result;
                progressBar.setVisibility(View.INVISIBLE);
                toValue = decimalFormat.format(amount);
                offerUpdateText.removeCallbacks(updateOffer);
                autoUpdateTime = 0;
                offerUpdateText.post(updateOffer);
                updateUI();
            }
        }

        @Override
        public void onFailure(@NonNull Call<ChangellyAPIService.ChangellyAnswerDouble> call,
                              @NonNull Throwable t) {
            Toast.makeText(getActivity(), "Service unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    class GetOfferCallback implements Callback<ChangellyTransaction> {
        @Override
        public void onResponse(@NonNull Call<ChangellyTransaction> call,
                               @NonNull Response<ChangellyTransaction> response) {
            ChangellyTransaction result = response.body();
            if(result != null) {
                buttonContinue.setEnabled(true);
                offer = result.result;
                updateUI();
            }
        }

        @Override
        public void onFailure(@NonNull Call<ChangellyTransaction> call, @NonNull Throwable t) {
        }
    }
}
