package com.mycelium.wallet.external.changelly.bch;


import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mycelium.spvmodule.IntentContract;
import com.mycelium.spvmodule.TransactionFee;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.WalletApplication;
import com.mycelium.wallet.event.SpvSendFundsResult;
import com.mycelium.wallet.external.changelly.ChangellyAPIService;
import com.mycelium.wallet.external.changelly.ChangellyService;
import com.mycelium.wallet.external.changelly.Constants;
import com.mycelium.wallet.external.changelly.ExchangeLoggingService;
import com.mycelium.wallet.external.changelly.model.Order;
import com.mycelium.wallet.pdf.BCHExchangeReceiptBuilder;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.bip44.Bip44BCHAccount;
import com.mycelium.wapi.wallet.currency.ExactBitcoinCashValue;
import com.mycelium.wapi.wallet.single.SingleAddressBCHAccount;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

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

public class ConfirmExchangeFragment extends Fragment {
    public static final int MINER_FEE = 450;
    public static final String TAG = "BCHExchange";

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

    MbwManager mbwManager;
    WalletAccount fromAccount;
    WalletAccount toAccount;
    Double amount;

    private ChangellyAPIService.ChangellyTransactionOffer offer;
    private ProgressDialog progressDialog;
    private Receiver receiver;

    private String lastOperationId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        UUID toAddress = (UUID) getArguments().getSerializable(Constants.DESTADDRESS);
        UUID fromAddress = (UUID) getArguments().getSerializable(Constants.FROM_ADDRESS);
        amount = getArguments().getDouble(Constants.FROM_AMOUNT);
        mbwManager = MbwManager.getInstance(getActivity());
        mbwManager.getEventBus().register(this);
        fromAccount = mbwManager.getWalletManager(false).getAccount(fromAddress);
        toAccount = mbwManager.getWalletManager(false).getAccount(toAddress);
        receiver = new Receiver();
        for (String action : new String[]{ChangellyService.INFO_TRANSACTION, ChangellyService.INFO_ERROR}) {
            IntentFilter intentFilter = new IntentFilter(action);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, intentFilter);
        }
        createOffer();
    }

    @OnClick(R.id.buttonContinue)
    void createAndSignTransaction() {
        long fromValue = ExactBitcoinCashValue.from(BigDecimal.valueOf(offer.amountFrom)).getLongValue();

        this.lastOperationId = UUID.randomUUID().toString();
        WalletAccount account = mbwManager.getSelectedAccount();

        switch (account.getType()) {
            case BCHBIP44: {
                Bip44BCHAccount bip44BCHAccount = (Bip44BCHAccount) account;
                Intent serviceIntent = IntentContract.SendFunds.createIntent(lastOperationId, bip44BCHAccount.getAccountIndex(), offer.payinAddress, fromValue, TransactionFee.NORMAL, (float) 1.0);
                WalletApplication.sendToSpv(serviceIntent, WalletAccount.Type.BCHBIP44);
                break;
            }
            case BCHSINGLEADDRESS: {
                SingleAddressBCHAccount bip44BCHAccount = (SingleAddressBCHAccount) account;
                Intent service = IntentContract.SendFundsSingleAddress.createIntent(lastOperationId, bip44BCHAccount.getId().toString(), offer.payinAddress, fromValue, TransactionFee.NORMAL, (float) 1.0);
                WalletApplication.sendToSpv(service, WalletAccount.Type.BCHSINGLEADDRESS);
                break;
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exchage_confirm, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fromAddress.setText(fromAccount.getReceivingAddress().get().toString());
        toAddress.setText(toAccount.getReceivingAddress().get().toString());

        fromLabel.setText(mbwManager.getMetadataStorage().getLabelByAccount(fromAccount.getId()));
        toLabel.setText(mbwManager.getMetadataStorage().getLabelByAccount(toAccount.getId()));
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        super.onDestroy();
    }

    private void createOffer() {
        Intent changellyServiceIntent = new Intent(getActivity(), ChangellyService.class)
                .setAction(ChangellyService.ACTION_CREATE_TRANSACTION)
                .putExtra(ChangellyService.FROM, ChangellyService.BCH)
                .putExtra(ChangellyService.TO, ChangellyService.BTC)
                .putExtra(ChangellyService.AMOUNT, amount)
                .putExtra(ChangellyService.DESTADDRESS, toAccount.getReceivingAddress().get().toString());
        getActivity().startService(changellyServiceIntent);
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Waiting offer...");
        progressDialog.show();
    }

    private void updateUI() {
        if (isAdded()) {
            fromAmount.setText(getString(R.string.value_currency, offer.currencyFrom, offer.amountFrom));
            toAmount.setText(getString(R.string.value_currency, offer.currencyTo, offer.amountTo));
        }
    }

    class Receiver extends BroadcastReceiver {
        private Receiver() {
        }  // prevents instantiation

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ChangellyService.INFO_TRANSACTION:
                    progressDialog.dismiss();
                    offer = (ChangellyAPIService.ChangellyTransactionOffer) intent.getSerializableExtra(ChangellyService.OFFER);
                    updateUI();
                    break;
                case INFO_ERROR:
                    progressDialog.dismiss();
                    new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.exchange_service_unavailable)
                            .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    getFragmentManager().popBackStack();
                                }
                            }).create().show();
                    break;
            }
        }
    }

    @Subscribe
    public void spvSendFundsResult(SpvSendFundsResult event) {
        if (!event.operationId.equals(lastOperationId))
            return;

        if (!event.isSuccess) {
            //TODO most probably we need UI dialog here to display the message
            Log.e(TAG, "Send funds failed: " + event.message);
            return;
        }
        final Order order = new Order();
        order.transactionId = event.txHash;
        order.exchangingAmount = String.valueOf(offer.amountFrom);
        order.exchangingCurrency = "BCH";
        order.receivingAddress = toAccount.getReceivingAddress().get().toString();
        order.receivingAmount = String.valueOf(offer.amountTo);
        order.receivingCurrency = "BTC";
        order.timestamp = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.LONG, Locale.ENGLISH)
                .format(new Date());

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.success)
                .setMessage(Html.fromHtml(getString(R.string.exchange_order_placed_dialog
                        , order.timestamp
                        , order.transactionId
                        , order.exchangingAmount
                        , order.receivingAmount)))
                .setPositiveButton(R.string.save_receipt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String pdf = new BCHExchangeReceiptBuilder()
                                .setTransactionId(order.transactionId)
                                .setDate(order.timestamp)
                                .setReceivingAmount(order.receivingAmount + " " + order.receivingCurrency)
                                .setReceivingAddress(order.receivingAddress)
                                .setSpendingAmount(order.exchangingAmount + " " + order.exchangingCurrency)
                                .setSpendingAccountLabel(mbwManager.getMetadataStorage().getLabelByAccount(fromAccount.getId()))
                                .build();
                        String filePart = new SimpleDateFormat( "yyMMddHHmmss").format(new Date());
                        File pdfFile = new File(getActivity().getExternalFilesDir(DIRECTORY_DOWNLOADS), "exchange_bch_order_" + filePart + ".pdf");
                        try {
                            OutputStream pdfStream = new FileOutputStream(pdfFile);
                            pdfStream.write(pdf.getBytes("UTF-8"));
                            pdfStream.close();
                        } catch (IOException e) {
                            Log.e(TAG, "", e);
                        }
                        DownloadManager downloadManager = (DownloadManager) getActivity().getSystemService(DOWNLOAD_SERVICE);
                        downloadManager.addCompletedDownload(pdfFile.getName(), pdfFile.getName()
                                , true, "application/pdf"
                                , pdfFile.getAbsolutePath(), pdfFile.length(), true);
                    }
                })
                .setNegativeButton(R.string.close, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        getActivity().finish();
                    }
                })
                .create().show();

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
