package com.mycelium.wapi.wallet.bch.bip44;

import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.wallet.btc.Bip44AccountBacking;
import com.mycelium.wapi.wallet.SpvBalanceFetcher;
import com.mycelium.wapi.wallet.btc.bip44.Bip44AccountContext;
import com.mycelium.wapi.wallet.btc.bip44.Bip44AccountKeyManager;

public class Bip44BCHPubOnlyAccount extends Bip44BCHAccount {
    public Bip44BCHPubOnlyAccount(Bip44AccountContext context, Bip44AccountKeyManager keyManager,
                                  NetworkParameters network, Bip44AccountBacking backing, Wapi wapi,
                                  SpvBalanceFetcher spvBalanceFetcher) {
        super(context, keyManager, network, backing, wapi, spvBalanceFetcher);
    }

    @Override
    public boolean canSpend() {
        return false;
    }
}
