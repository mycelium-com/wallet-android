package com.mycelium.wapi.wallet.single;

import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.wallet.SingleAddressAccountBacking;
import com.mycelium.wapi.wallet.SpvBalanceFetcher;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;

public class SingleAddressBCHAccount extends SingleAddressAccount {

    private SpvBalanceFetcher spvBalanceFetcher;

    public SingleAddressBCHAccount(SingleAddressAccountContext context, PublicPrivateKeyStore keyStore, NetworkParameters network, SingleAddressAccountBacking backing, Wapi wapi, SpvBalanceFetcher spvBalanceFetcher) {
        super(context, keyStore, network, backing, wapi);
        this.spvBalanceFetcher = spvBalanceFetcher;
        this.type = Type.BCHSINGLEADDRESS;
    }

    @Override
    public CurrencyBasedBalance getCurrencyBasedBalance() {
        return spvBalanceFetcher.retrieveBySingleAddressAccountId(getId().toString());
    }
}
