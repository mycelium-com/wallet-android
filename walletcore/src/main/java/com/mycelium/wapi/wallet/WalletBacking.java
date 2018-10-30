package com.mycelium.wapi.wallet;

import java.util.List;
import java.util.UUID;

public interface WalletBacking<AccountContext, T extends GenericTransaction> {
    List<AccountContext> loadAccountContexts();

    AccountBacking<T> getAccountBacking(UUID accountId);

    void createAccountContext(AccountContext context);

    void updateAccountContext(AccountContext context);

    void deleteAccountContext(UUID uuid);
}
