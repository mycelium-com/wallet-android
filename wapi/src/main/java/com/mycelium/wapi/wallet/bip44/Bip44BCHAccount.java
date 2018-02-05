package com.mycelium.wapi.wallet.bip44;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.Transaction;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.Bip44AccountBacking;
import com.mycelium.wapi.wallet.SpvBalanceFetcher;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Bip44BCHAccount extends Bip44Account {
    private SpvBalanceFetcher spvBalanceFetcher;
    private int blockChainHeight;
    private boolean visible;

    public Bip44BCHAccount(Bip44AccountContext context, Bip44AccountKeyManager keyManager, NetworkParameters network, Bip44AccountBacking backing, Wapi wapi, SpvBalanceFetcher spvBalanceFetcher) {
        super(context, keyManager, network, backing, wapi);
        this.spvBalanceFetcher = spvBalanceFetcher;
        this.type = Type.BCHBIP44;
    }

    @Override
    public CurrencyBasedBalance getCurrencyBasedBalance() {
        return spvBalanceFetcher.retrieveByHdAccountIndex(getId().toString(), getAccountIndex());
    }

    @Override
    public UUID getId() {
        return UUID.nameUUIDFromBytes(("BCH" + super.getId().toString()).getBytes());
    }

    // need override because parent write it to context(bch and btc account have one context)
    @Override
    public void setBlockChainHeight(int blockHeight) {
        blockChainHeight = blockHeight;
    }

    @Override
    public int getBlockChainHeight() {
        return blockChainHeight;
    }

    @Override
    public List<TransactionSummary> getTransactionHistory(int offset, int limit) {
        return spvBalanceFetcher.retrieveTransactionSummaryByHdAccountIndex(getId().toString(), getAccountIndex());
    }

    @Override
    public List<TransactionSummary> getTransactionsSince(Long receivingSince) {
        return spvBalanceFetcher.retrieveTransactionSummaryByHdAccountIndex(getId().toString(), getAccountIndex());
    }

    @Override
    public boolean isVisible() {
        if (!visible) {
            visible = !spvBalanceFetcher.retrieveTransactionSummaryByHdAccountIndex(getId().toString(), getAccountIndex()).isEmpty();
        }
        return visible;
    }

    @Override
    public int getPrivateKeyCount() {
        return spvBalanceFetcher.getPrivateKeysCount(getAccountIndex());
    }

    @Override
    public Optional<Address> getReceivingAddress() {
        return Optional.fromNullable(spvBalanceFetcher.getCurrentReceiveAddress(getAccountIndex()));
    }
}
