package com.mycelium.wapi.wallet.bip44;

import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.wallet.Bip44AccountBacking;
import com.mycelium.wapi.wallet.SpvBalanceFetcher;

public class Bip44BCHPubOnlyAccount extends Bip44BCHAccount {
    public Bip44BCHPubOnlyAccount(Bip44AccountContext context, Bip44AccountKeyManager keyManager, NetworkParameters network, Bip44AccountBacking backing, Wapi wapi, SpvBalanceFetcher spvBalanceFetcher) {
        super(context, keyManager, network, backing, wapi, spvBalanceFetcher);
    }

    @Override
    public boolean canSpend() {
        return false;
    }
}
