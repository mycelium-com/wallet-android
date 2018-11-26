package com.mycelium;

import com.mycelium.wapi.wallet.AccountBacking;
import com.mycelium.wapi.wallet.WalletBacking;
import com.mycelium.wapi.wallet.colu.ColuAccountContext;
import com.mycelium.wapi.wallet.colu.ColuTransaction;

import java.util.List;
import java.util.UUID;

public class ColuWalletBacking implements WalletBacking<ColuAccountContext,ColuTransaction> {
    @Override
    public List<ColuAccountContext> loadAccountContexts() {
        return null;
    }

    @Override
    public AccountBacking<ColuTransaction> getAccountBacking(UUID accountId) {
        return new ColuAccountBacking();
    }

    @Override
    public void createAccountContext(ColuAccountContext coluAccountContext) {

    }

    @Override
    public void updateAccountContext(ColuAccountContext coluAccountContext) {

    }

    @Override
    public void deleteAccountContext(UUID uuid) {

    }
}
