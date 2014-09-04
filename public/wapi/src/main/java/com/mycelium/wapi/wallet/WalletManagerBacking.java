package com.mycelium.wapi.wallet;

import com.mycelium.wapi.wallet.bip44.Bip44AccountContext;
import com.mycelium.wapi.wallet.single.SingleAddressAccountContext;

import java.util.List;
import java.util.UUID;

public interface WalletManagerBacking extends SecureKeyValueStoreBacking {

   void beginTransaction();

   void setTransactionSuccessful();

   void endTransaction();

   List<Bip44AccountContext> loadBip44AccountContexts();

   void createBip44AccountContext(Bip44AccountContext context);

   List<SingleAddressAccountContext> loadSingleAddressAccountContexts();

   void createSingleAddressAccountContext(SingleAddressAccountContext context);

   void deleteSingleAddressAccountContext(UUID accountId);

   Bip44AccountBacking getBip44AccountBacking(UUID accountId);

   SingleAddressAccountBacking getSingleAddressAccountBacking(UUID accountId);
}