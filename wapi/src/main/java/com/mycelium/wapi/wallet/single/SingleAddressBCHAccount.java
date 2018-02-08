package com.mycelium.wapi.wallet.single;

import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.SingleAddressAccountBacking;
import com.mycelium.wapi.wallet.SpvBalanceFetcher;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;
import com.mycelium.wapi.wallet.currency.CurrencyValue;

import java.util.List;
import java.util.UUID;

public class SingleAddressBCHAccount extends SingleAddressAccount {
    private SpvBalanceFetcher spvBalanceFetcher;
    private boolean visible;

    public SingleAddressBCHAccount(SingleAddressAccountContext context, PublicPrivateKeyStore keyStore, NetworkParameters network, SingleAddressAccountBacking backing, Wapi wapi, SpvBalanceFetcher spvBalanceFetcher) {
        super(context, keyStore, network, backing, wapi);
        this.spvBalanceFetcher = spvBalanceFetcher;
        this.type = Type.BCHSINGLEADDRESS;
    }

    @Override
    public String getAccountDefaultCurrency() {
        return CurrencyValue.BCH;
    }

    @Override
    public CurrencyBasedBalance getCurrencyBasedBalance() {
        return spvBalanceFetcher.retrieveBySingleAddressAccountId(getId().toString());
    }

    @Override
    public UUID getId() {
        return UUID.nameUUIDFromBytes(("BCH" + super.getId().toString()).getBytes());
    }

    @Override
    public List<TransactionSummary> getTransactionHistory(int offset, int limit) {
        return spvBalanceFetcher.retrieveTransactionSummaryBySingleAddressAccountId(getId().toString());
    }

    @Override
    public List<TransactionSummary> getTransactionsSince(Long receivingSince) {
        return spvBalanceFetcher.retrieveTransactionSummaryBySingleAddressAccountId(getId().toString(), receivingSince);
    }

    @Override
    public boolean isVisible() {
        if (!visible) {
            visible = !spvBalanceFetcher.retrieveTransactionSummaryBySingleAddressAccountId(getId().toString()).isEmpty();
        }
        return visible;
    }
}
