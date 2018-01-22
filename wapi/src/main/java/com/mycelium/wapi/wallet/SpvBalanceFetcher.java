package com.mycelium.wapi.wallet;

import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;

import java.util.List;

public interface SpvBalanceFetcher {
    CurrencyBasedBalance retrieveByHdAccountIndex(String id, int accountIndex);
    CurrencyBasedBalance retrieveBySingleAddressAccountId(String id);
    List<TransactionSummary> retrieveTransactionSummaryByHdAccountIndex(String id, int accountIndex);
    List<TransactionSummary> retrieveTransactionSummaryBySingleAddressAccountId(String id);
    void requestTransactionsAsync(int accountId);
    void requestTransactionsFromSingleAddressAccountAsync(String guid);
    void requestSingleAddressWalletAccountRemoval(String guid);
}
