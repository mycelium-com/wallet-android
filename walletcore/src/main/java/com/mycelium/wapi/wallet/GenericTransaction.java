package com.mycelium.wapi.wallet;

import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.wallet.coins.CoinType;
import com.mycelium.wapi.wallet.coins.Value;

import java.util.List;

public interface GenericTransaction {
    class GenericOutput {
        final GenericAddress genericAddress;
        final Value value;

        public GenericOutput(GenericAddress genericAddress, Value value) {
            this.genericAddress = genericAddress;
            this.value = value;
        }

        public GenericAddress getAddress() {
            return genericAddress;
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

    List<GenericAddress> getReceivedFrom();
    List<GenericOutput> getSentTo();

    Value getSent();
    Value getReceived();
    boolean isIncoming();
}
