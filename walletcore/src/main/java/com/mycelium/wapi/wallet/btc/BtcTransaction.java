package com.mycelium.wapi.wallet.btc;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.wallet.ConfirmationRiskProfileLocal;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.GenericTransaction;
import com.mycelium.wapi.wallet.coins.CoinType;
import com.mycelium.wapi.wallet.coins.Value;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BtcTransaction implements GenericTransaction {
    final CoinType type;
    final Sha256Hash hash;
    final Transaction tx;
    final Value valueSent;
    final Value valueReceived;
    final Value value;
    private int timestamp;
    final ArrayList<GenericInput> inputs;
    final ArrayList<GenericOutput> outputs;
    private int confirmations;
    final int rawSize;
    private final boolean isQueuedOutgoing;
    public final Optional<ConfirmationRiskProfileLocal> confirmationRiskProfile;
    @Nullable
    final Value fee;

    public BtcTransaction(CoinType type, Transaction transaction,
                          long valueSent, long valueReceived, int timestamp, int confirmations,
                          boolean isQueuedOutgoing, ArrayList<GenericInput> inputs,
                          ArrayList<GenericOutput> outputs, ConfirmationRiskProfileLocal risk,
                          int rawSize, @Nullable Value fee) {
        this.type = type;
        this.tx = transaction;
        this.hash = tx.getId();
        this.valueSent = Value.valueOf(type, valueSent);
        this.valueReceived = Value.valueOf(type, valueReceived);
        this.value = this.valueReceived.subtract(this.valueSent);
        this.timestamp = timestamp;
        this.confirmations = confirmations;
        this.isQueuedOutgoing = isQueuedOutgoing;
        this.inputs = inputs;
        this.outputs = outputs;
        this.confirmationRiskProfile = Optional.fromNullable(risk);
        this.rawSize = rawSize;
        this.fee = fee;
    }

    @Override
    public boolean isQueuedOutgoing() {
        return isQueuedOutgoing;
    }

    @Override
    public CoinType getType() {
        return type;
    }

    @Override
    public int getAppearedAtChainHeight() {
        return confirmations;
    }

    @Override
    public void setAppearedAtChainHeight(int appearedAtChainHeight) {
        this.confirmations = appearedAtChainHeight;
    }

    @Override
    public int getDepthInBlocks() {
        return 0;
    }

    @Override
    public void setDepthInBlocks(int depthInBlocks) {
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }


    @Override
    @Nullable
    public Value getFee() {
        return fee;
    }

    @Override
    public List<GenericInput> getInputs() {
        return inputs;
    }

    @Override
    public List<GenericOutput> getOutputs() {
        return outputs;
    }

    @Override
    public Value getSent() {
        return valueSent;
    }

    @Override
    public Value getReceived() {
        return valueReceived;
    }

    @Override
    public boolean isIncoming() {
        return value.value >= 0;
    }

    @Override
    public int getRawSize() {
        return rawSize;
    }

    @Override
    public Sha256Hash getHash() {
        // TODO: Find out should we return tx.getHash() or tx.getId().
        // This is related with latest SEGWIT changes
        return tx.getId();
    }

    @Override
    public String getHashAsString() {
        return getHash().toString();
    }

    @Override
    public byte[] getHashBytes() {
        return getHash().getBytes();
    }

    @Override
    public Optional<ConfirmationRiskProfileLocal> getConfirmationRiskProfile() {
        return confirmationRiskProfile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BtcTransaction other = (BtcTransaction) o;
        return getHash().equals(other.getHash());
    }

    public Transaction getRawTransaction() {
        return tx;
    }

    @Override
    public String toString(){
        return hash.toString();
    }
}
