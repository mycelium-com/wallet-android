package com.mycelium.wallet.modularisation;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;

import com.mycelium.spvmodule.providers.TransactionContract;
import com.mycelium.wapi.wallet.SpvBalanceFetcher;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;
import com.mycelium.wapi.wallet.currency.ExactBitcoinCashValue;
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

        return new CurrencyBasedBalance(ExactBitcoinCashValue.from(confirmed),
                ExactBitcoinCashValue.from(sending), ExactBitcoinCashValue.from(receiving));
    }

    @Override
    public CurrencyBasedBalance retrieveByHdAccountIndex(String id, int accountIndex) {
        CurrencyBasedBalance balance = CurrencyBasedBalance.ZERO_BITCOIN_CASH_BALANCE;
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
        CurrencyBasedBalance balance = CurrencyBasedBalance.ZERO_BITCOIN_CASH_BALANCE;
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
}
