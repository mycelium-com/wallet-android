package com.mycelium.wallet.coinapult;

import com.mycelium.wapi.wallet.CommonAccountBacking;
import com.mycelium.wapi.wallet.WalletBacking;
import com.mycelium.wapi.wallet.coinapult.CoinapultAccountContext;

import java.util.List;
import java.util.UUID;

public class SQLiteCoinapultBacking implements WalletBacking<CoinapultAccountContext> {

    @Override
    public List<CoinapultAccountContext> loadAccountContexts() {
        return null;
    }

    @Override
    public CommonAccountBacking getAccountBacking(UUID accountId) {
        return null;
    }

    @Override
    public void createAccountContext(CoinapultAccountContext coinapultAccountContext) {

    }

    @Override
    public void updateAccountContext(CoinapultAccountContext coinapultAccountContext) {

    }

    @Override
    public void deleteAccountContext(UUID uuid) {

    }
}
