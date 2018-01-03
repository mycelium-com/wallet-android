package com.mycelium.wallet.external.changelly.bch;


import android.app.AlertDialog;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.mrd.bitlib.StandardTransactionBuilder.OutputTooSmallException;
import com.mrd.bitlib.StandardTransactionBuilder.UnableToBuildTransactionException;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.Transaction;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.external.changelly.ChangellyAPIService;
import com.mycelium.wallet.external.changelly.ChangellyService;
import com.mycelium.wallet.external.changelly.Constants;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.currency.ExactBitcoinCashValue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.mrd.bitlib.StandardTransactionBuilder.estimateTransactionSize;
import static com.mycelium.wallet.external.changelly.ChangellyService.INFO_ERROR;

public class ConfirmExchangeFragment extends Fragment {
    public static final int MINER_FEE = 450;

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        UUID toAddress = (UUID) getArguments().getSerializable(Constants.DESTADDRESS);
        UUID fromAddress = (UUID) getArguments().getSerializable(Constants.FROM_ADDRESS);
        amount = getArguments().getDouble(Constants.FROM_AMOUNT);
        mbwManager = MbwManager.getInstance(getActivity());
        fromAccount = mbwManager.getWalletManager(false).getAccount(fromAddress);
        toAccount = mbwManager.getWalletManager(false).getAccount(toAddress);
        receiver = new Receiver();
        for (String action : new String[]{ChangellyService.INFO_TRANSACTION, ChangellyService.INFO_ERROR}) {
            IntentFilter intentFilter = new IntentFilter(action);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, intentFilter);
        }
        createOffer();
    }

    void createAndSignTransaction() {
        long fromValue = ExactBitcoinCashValue.from(BigDecimal.valueOf(offer.amountFrom)).getLongValue();
        try {
            StandardTransactionBuilder.UnsignedTransaction unsignedTransaction = fromAccount.createUnsignedTransaction(
                    Arrays.asList(new WalletAccount.Receiver(Address.fromString(offer.payinAddress), fromValue))
                    , MINER_FEE);
            Transaction transaction = fromAccount.signTransaction(unsignedTransaction, AesKeyCipher.defaultKeyCipher());

            //TODO braodcast BCH transaction
        } catch (UnableToBuildTransactionException | InsufficientFundsException | OutputTooSmallException | KeyCipher.InvalidKeyCipher e) {
            e.printStackTrace();
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

    private int estimateTxSize() {
        int inCount = 1;
        int outCount = 2;
        return estimateTransactionSize(inCount, outCount);
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
                            .setMessage("Exchange service not available now, try later")
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
}
