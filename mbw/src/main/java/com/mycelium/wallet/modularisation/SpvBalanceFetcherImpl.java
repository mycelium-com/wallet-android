package com.mycelium.wallet.modularisation;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.mycelium.spvmodule.IntentContract;
import com.mycelium.spvmodule.providers.TransactionContract;
import com.mycelium.wallet.WalletApplication;
import com.mycelium.wapi.wallet.SpvBalanceFetcher;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;
import com.mycelium.wapi.wallet.currency.ExactBitcoinValue;

import static com.mycelium.wallet.WalletApplication.getSpvModuleName;


public class SpvBalanceFetcherImpl implements SpvBalanceFetcher {

    private ContentResolver contentResolver;

    public SpvBalanceFetcherImpl(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    private CurrencyBasedBalance from(Cursor cursor) {
        // String id = cursor.getString(cursor.getColumnIndex(TransactionContract.AccountBalance._ID));
        Long confirmed = cursor.getLong(cursor.getColumnIndex(TransactionContract.AccountBalance.CONFIRMED));
        Long receiving = cursor.getLong(cursor.getColumnIndex(TransactionContract.AccountBalance.RECEIVING));
        Long sending = cursor.getLong(cursor.getColumnIndex(TransactionContract.AccountBalance.SENDING));

        return new CurrencyBasedBalance(ExactBitcoinValue.from(confirmed),
                ExactBitcoinValue.from(sending), ExactBitcoinValue.from(receiving));
    }

    @Override
    public CurrencyBasedBalance retrieveByHdAccountIndex(String id, int accountIndex) {
        CurrencyBasedBalance balance = CurrencyBasedBalance.ZERO_BITCOIN_BALANCE;
        Uri uri = TransactionContract.AccountBalance.CONTENT_URI(getSpvModuleName()).buildUpon().appendEncodedPath(id).build();
        String selection = TransactionContract.AccountBalance.SELECTION_ACCOUNT_INDEX;
        String[] selectionArgs = new String[]{Integer.toString(accountIndex)};
        Cursor cursor = null;

        try {
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null);
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

    @Override
    public CurrencyBasedBalance retrieveBySingleAddressAccountId(String id) {
        CurrencyBasedBalance balance = CurrencyBasedBalance.ZERO_BITCOIN_BALANCE;
        String selection = TransactionContract.AccountBalance.SELECTION_SINGLE_ADDRESS_ACCOUNT_GUID;
        String[] selectionArgs = new String[]{id};
        Uri uri = TransactionContract.AccountBalance.CONTENT_URI(getSpvModuleName()).buildUpon().appendEncodedPath(id).build();

        Cursor cursor = null;

        try {
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null);
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

    public void getTransactions(int accountId) {
        Intent service = IntentContract.ReceiveTransactions.createIntent(accountId);
        WalletApplication.sendToSpv(service);
    }

    public void getTransactionsFromSingleAddressAccount(String guid) {
        Intent service = IntentContract.ReceiveTransactionsSingleAddress.createIntent(guid);
        WalletApplication.sendToSpv(service);
    }
}
