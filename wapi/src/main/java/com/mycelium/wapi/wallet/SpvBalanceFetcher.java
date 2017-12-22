package com.mycelium.wapi.wallet;

import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;

public interface SpvBalanceFetcher {
    CurrencyBasedBalance retrieveByHdAccountIndex(String id, int accountIndex);
    CurrencyBasedBalance retrieveBySingleAddressAccountId(String id);
}
