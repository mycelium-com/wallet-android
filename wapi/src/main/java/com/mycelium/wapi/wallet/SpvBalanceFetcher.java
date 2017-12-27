package com.mycelium.wapi.wallet;

import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;

public interface SpvBalanceFetcher {
    CurrencyBasedBalance retrieveByHdAccountIndex(String id, int accountIndex);
    CurrencyBasedBalance retrieveBySingleAddressAccountId(String id);
    void getTransactions(int accountId);
    void getTransactionsFromSingleAddressAccount(String guid);
    boolean isActive();
}
