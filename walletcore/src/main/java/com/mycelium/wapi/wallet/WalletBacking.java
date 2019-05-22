package com.mycelium.wapi.wallet;

import java.util.List;
import java.util.UUID;

public interface WalletBacking<AccountContext> {
    List<AccountContext> loadAccountContexts();

    CommonAccountBacking getAccountBacking(UUID accountId);

    void createAccountContext(AccountContext context);

    void updateAccountContext(AccountContext context);

    void deleteAccountContext(UUID uuid);
}
