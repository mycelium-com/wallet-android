package com.mycelium.wapi.wallet;

import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.wallet.coins.CoinType;
import com.mycelium.wapi.wallet.coins.Value;

import java.util.List;

public interface AbstractTransaction {
    class AbstractOutput {
        final AbstractAddress abstractAddress;
        final Value value;

        public AbstractOutput(AbstractAddress abstractAddress, Value value) {
            this.abstractAddress = abstractAddress;
            this.value = value;
        }

        public AbstractAddress getAddress() {
            return abstractAddress;
        }

        public Value getValue() {
            return value;
        }
    }

    CoinType getType();

    Sha256Hash getHash();
    String getHashAsString();
    byte[] getHashBytes();

    int getDepthInBlocks();
    void setDepthInBlocks(int depthInBlocks);

    int getAppearedAtChainHeight();
    void setAppearedAtChainHeight(int appearedAtChainHeight);

    long getTimestamp();
    void setTimestamp(long timestamp);

    Value getFee();

    List<AbstractAddress> getReceivedFrom();
    List<AbstractOutput> getSentTo();
}
