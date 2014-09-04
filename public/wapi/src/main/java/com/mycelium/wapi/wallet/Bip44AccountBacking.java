package com.mycelium.wapi.wallet;

import com.mycelium.wapi.wallet.bip44.Bip44AccountContext;

public interface Bip44AccountBacking extends AccountBacking {

   void updateAccountContext(Bip44AccountContext context);

}