package com.mycelium.wallet.modularisation;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.spvmodule.IntentContract;
import com.mycelium.spvmodule.providers.TransactionContract;
import com.mycelium.wallet.WalletApplication;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.ConfirmationRiskProfileLocal;
import com.mycelium.wapi.wallet.SpvBalanceFetcher;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;
import com.mycelium.wapi.wallet.currency.ExactBitcoinCashValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinValue;

import java.util.ArrayList;
import java.util.List;

import static com.mycelium.wallet.WalletApplication.getSpvModuleName;

public class SpvBchFetcher implements SpvBalanceFetcher {
    private Context context;

    public SpvBchFetcher(Context context) {
        this.context = context;
    }

    @Override
    public CurrencyBasedBalance retrieveByHdAccountIndex(String id, int accountIndex) {
        Uri uri = TransactionContract.AccountBalance.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHBIP44)).buildUpon().appendEncodedPath(id).build();
        String selection = TransactionContract.AccountBalance.SELECTION_ACCOUNT_INDEX;
        return retrieveBalance(uri, selection, "" + accountIndex);
    }

    @Override
    public CurrencyBasedBalance retrieveBySingleAddressAccountId(String id) {
        Uri uri = TransactionContract.AccountBalance.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHSINGLEADDRESS)).buildUpon().appendEncodedPath(id).build();
        String selection = TransactionContract.AccountBalance.SELECTION_SINGLE_ADDRESS_ACCOUNT_GUID;
        return retrieveBalance(uri, selection, id);
    }

    private CurrencyBasedBalance retrieveBalance(Uri uri, String selection, String selectionArg) {
        CurrencyBasedBalance balance = CurrencyBasedBalance.ZERO_BITCOIN_CASH_BALANCE;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, selection, new String[]{selectionArg}, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    balance = from(cursor);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return balance;
    }

    private CurrencyBasedBalance from(Cursor cursor) {
        // String id = cursor.getString(cursor.getColumnIndex(TransactionContract.AccountBalance._ID));
        Long confirmed = cursor.getLong(cursor.getColumnIndex(TransactionContract.AccountBalance.CONFIRMED));
        Long receiving = cursor.getLong(cursor.getColumnIndex(TransactionContract.AccountBalance.RECEIVING));
        Long sending = cursor.getLong(cursor.getColumnIndex(TransactionContract.AccountBalance.SENDING));

        return new CurrencyBasedBalance(ExactBitcoinCashValue.from(confirmed),
                ExactBitcoinCashValue.from(sending), ExactBitcoinCashValue.from(receiving));
    }

    private TransactionSummary txSummaryFromCursor(Cursor cursor) {

        String txId = cursor.getString(cursor.getColumnIndex(TransactionContract.TransactionSummary._ID));
        Long valueSatoshis = cursor.getLong(cursor.getColumnIndex(TransactionContract.TransactionSummary.VALUE));
        int isIncoming = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.IS_INCOMING));
        Long time = cursor.getLong(cursor.getColumnIndex(TransactionContract.TransactionSummary.TIME));
        Integer height = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.HEIGHT));
        Integer confirmations = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.CONFIRMATIONS));
        Integer isQueuedOutgoing = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.IS_QUEUED_OUTGOING));
        Integer unconfirmedChainLength = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.CONFIRMATION_RISK_PROFILE_LENGTH));
        Integer hasRpfRisk = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.CONFIRMATION_RISK_PROFILE_RBF_RISK));
        Integer hasDoubleSpend = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.CONFIRMATION_RISK_PROFILE_DOUBLE_SPEND));
        String destinationAddress = cursor.getString(cursor.getColumnIndex(TransactionContract.TransactionSummary.DESTINATION_ADDRESS));
        String toAddresses = cursor.getString(cursor.getColumnIndex(TransactionContract.TransactionSummary.TO_ADDRESSES));

        return new TransactionSummary(Sha256Hash.fromString(txId),
                ExactBitcoinValue.from(valueSatoshis),
                (isIncoming != 0),
                time,
                height,
                confirmations,
                (isQueuedOutgoing != 0),
                new ConfirmationRiskProfileLocal(unconfirmedChainLength, (hasRpfRisk != 0), (hasDoubleSpend != 0)),
                Optional.of(Address.fromString(destinationAddress)),
                new ArrayList<Address>());
    }

    private List<TransactionSummary> retrieveTransactionSummary(Uri uri, String selection, String selectionArg) {
        List<TransactionSummary> transactionSummariesList = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, selection, new String[]{selectionArg}, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    TransactionSummary txSummary = txSummaryFromCursor(cursor);
                    transactionSummariesList.add(txSummary);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return transactionSummariesList;
    }

    public List<TransactionSummary> retrieveTransactionSummaryByHdAccountIndex(String id, int accountIndex)
    {
        Uri uri = TransactionContract.TransactionSummary.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHBIP44)).buildUpon().appendEncodedPath(id).build();
        String selection = TransactionContract.TransactionSummary.SELECTION_ACCOUNT_INDEX;
        return retrieveTransactionSummary(uri, selection, "" + accountIndex);

    }

    public List<TransactionSummary> retrieveTransactionSummaryBySingleAddressAccountId(String id)
    {
        Uri uri = TransactionContract.TransactionSummary.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHSINGLEADDRESS)).buildUpon().appendEncodedPath(id).build();
        String selection = TransactionContract.TransactionSummary.SELECTION_SINGLE_ADDRESS_ACCOUNT_GUID;
        return retrieveTransactionSummary(uri, selection, id);
    }

    public void requestTransactionsAsync(int accountId) {
        Intent service = IntentContract.ReceiveTransactions.createIntent(accountId);
        WalletApplication.sendToSpv(service, WalletAccount.Type.BCHBIP44);
    }

    public void requestTransactionsFromSingleAddressAccountAsync(String guid) {
        Intent service = IntentContract.ReceiveTransactionsSingleAddress.createIntent(guid);
        WalletApplication.sendToSpv(service, WalletAccount.Type.BCHSINGLEADDRESS);
    }
}
