package com.mycelium.wallet.external.changelly.bch;


import android.app.DownloadManager;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import com.mycelium.wallet.external.changelly.ChangellyService;
import com.mycelium.wallet.external.changelly.Constants;
import com.mycelium.wallet.external.changelly.ExchangeLoggingService;
import com.mycelium.wallet.external.changelly.model.Order;
import com.mycelium.wallet.pdf.BCHExchangeReceiptBuilder;
import com.mycelium.wapi.wallet.btc.WalletBtcAccount;
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinCashValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinValue;
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount;
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
import static com.mycelium.wallet.external.changelly.ChangellyService.INFO_ERROR;
import static com.mycelium.wallet.external.changelly.Constants.ABOUT;
import static com.mycelium.wallet.external.changelly.Constants.decimalFormat;
import static com.mycelium.wallet.external.changelly.bch.ExchangeFragment.BCH_EXCHANGE;
import static com.mycelium.wallet.external.changelly.bch.ExchangeFragment.BCH_EXCHANGE_TRANSACTIONS;
import static com.mycelium.wapi.wallet.btc.bip44.Bip44AccountContext.ACCOUNT_TYPE_FROM_MASTERSEED;

public class ConfirmExchangeFragment extends Fragment {
    public static final String TAG = "BCHExchange";
    public static final int UPDATE_TIME = 60;
    public static final String BLOCKTRAIL_TRANSACTION = "https://www.blocktrail.com/_network_/tx/_id_";

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
    WalletBtcAccount fromAccount;
    WalletBtcAccount toAccount;
    Double amount;
    Double sentAmount;

    private ChangellyAPIService.ChangellyTransactionOffer offer;
    private ProgressDialog progressDialog;
    private Receiver receiver;

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
        receiver = new Receiver();
        for (String action : new String[]{ChangellyService.INFO_TRANSACTION
                , ChangellyService.INFO_ERROR
                , ChangellyService.INFO_EXCH_AMOUNT}) {
            IntentFilter intentFilter = new IntentFilter(action);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, intentFilter);
        }
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
                        WalletApplication.sendToSpv(service, WalletBtcAccount.Type.BCHBIP44);
                        break;
                    }
                    case BCHSINGLEADDRESS: {
                        SingleAddressBCHAccount singleAddressAccount = (SingleAddressBCHAccount) fromAccount;
                        service = IntentContract.SendFundsUnrelated.createIntent(lastOperationId, singleAddressAccount.getId().toString(), payAddress, fromValue, TransactionFee.NORMAL, 1.0f, IntentContract.UNRELATED_ACCOUNT_TYPE_SA);
                        WalletApplication.sendToSpv(service, WalletBtcAccount.Type.BCHSINGLEADDRESS);
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

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        super.onDestroy();
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
        Intent changellyServiceIntent = new Intent(getActivity(), ChangellyService.class)
                .setAction(ChangellyService.ACTION_CREATE_TRANSACTION)
                .putExtra(ChangellyService.FROM, ChangellyService.BCH)
                .putExtra(ChangellyService.TO, ChangellyService.BTC)
                .putExtra(ChangellyService.AMOUNT, sentAmount)
                .putExtra(ChangellyService.DESTADDRESS, toAccount.getReceivingAddress().get().toString());
        getActivity().startService(changellyServiceIntent);

    }

    private void getRate() {
        Intent changellyServiceIntent = new Intent(getActivity(), ChangellyService.class)
                .setAction(ChangellyService.ACTION_GET_EXCHANGE_AMOUNT)
                .putExtra(ChangellyService.FROM, ChangellyService.BCH)
                .putExtra(ChangellyService.TO, ChangellyService.BTC)
                .putExtra(ChangellyService.AMOUNT, sentAmount);
        getActivity().startService(changellyServiceIntent);
    }

    private void updateUI() {
        if (isAdded()) {
            fromAmount.setText(getString(R.string.value_currency, decimalFormat.format(amount)
                    , ChangellyService.BCH));
            toAmount.setText(getString(R.string.value_currency, toValue
                    , ChangellyService.BTC));
            updateRate();
        }
    }

    class Receiver extends BroadcastReceiver {
        private Receiver() {
        }  // prevents instantiation

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ChangellyService.INFO_TRANSACTION:
                    buttonContinue.setEnabled(true);
                    offer = (ChangellyAPIService.ChangellyTransactionOffer) intent.getSerializableExtra(ChangellyService.OFFER);
                    updateUI();
                    break;
                case INFO_ERROR:
                    progressBar.setVisibility(View.INVISIBLE);
                    new AlertDialog.Builder(getActivity(), R.style.MyceliumModern_Dialog)
                            .setMessage(R.string.exchange_service_unavailable)
                            .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    getFragmentManager().popBackStack();
                                }
                            }).create().show();
                    break;
                case ChangellyService.INFO_EXCH_AMOUNT:
                    progressBar.setVisibility(View.INVISIBLE);
                    toValue = decimalFormat.format(intent.getDoubleExtra(ChangellyService.AMOUNT, 0));
                    offerUpdateText.removeCallbacks(updateOffer);
                    autoUpdateTime = 0;
                    offerUpdateText.post(updateOffer);
                    updateUI();
                    break;
            }
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
}
