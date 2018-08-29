package com.mycelium.wapi.wallet.fiat;

import com.google.common.base.Optional;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.wallet.ConfirmationRiskProfileLocal;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.GenericTransaction;
import com.mycelium.wapi.wallet.coins.CoinType;
import com.mycelium.wapi.wallet.coins.Value;

import java.util.List;

public class FiatTransaction implements GenericTransaction {

    @Override
    public CoinType getType() {
        return null;
    }

    @Override
    public Sha256Hash getHash() {
        return null;
    }

    @Override
    public String getHashAsString() {
        return null;
    }

    @Override
    public byte[] getHashBytes() {
        return new byte[0];
    }

    @Override
    public int getDepthInBlocks() {
        return 0;
    }

    @Override
    public void setDepthInBlocks(int depthInBlocks) {

    }

    @Override
    public int getAppearedAtChainHeight() {
        return 0;
    }

    @Override
    public void setAppearedAtChainHeight(int appearedAtChainHeight) {

    }

    @Override
    public long getTimestamp() {
        return 0;
    }

    @Override
    public void setTimestamp(int timestamp) {

    }

    @Override
    public boolean isQueuedOutgoing() {
        return false;
    }

    @Override
    public Optional<ConfirmationRiskProfileLocal> getConfirmationRiskProfile() {
        return null;
    }

    public void setTimestamp(long timestamp) {

    }

    @Override
    public Value getFee() {
        return null;
    }

    @Override
    public List<GenericAddress> getReceivedFrom() {
        return null;
    }

    @Override
    public List<GenericOutput> getSentTo() {
        return null;
    }

    @Override
    public Value getSent() {
        return null;
    }

    @Override
    public Value getReceived() {
        return null;
    }

    @Override
    public boolean isIncoming() {
        return false;
    }
}
