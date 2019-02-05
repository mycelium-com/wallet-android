package com.mycelium.wapi.wallet.btc;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.wallet.ConfirmationRiskProfileLocal;
import com.mycelium.wapi.wallet.GenericTransaction;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.GenericAssetInfo;
import com.mycelium.wapi.wallet.coins.Value;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class BtcTransaction implements GenericTransaction, Serializable {
    protected CryptoCurrency type;
    protected Sha256Hash hash;
    private Transaction tx;
    protected Value transferred;
    protected int timestamp;
    protected ArrayList<GenericInput> inputs;
    protected ArrayList<GenericOutput> outputs;
    protected int height;
    protected int confirmations;
    protected int rawSize;
    protected boolean isQueuedOutgoing;
    public  Optional<ConfirmationRiskProfileLocal> confirmationRiskProfile;
    @Nullable
    protected Value fee;

    public BtcTransaction(CryptoCurrency type, Transaction transaction) {
        this.type = type;
        this.tx = transaction;
        this.hash = tx.getId();
        this.transferred = Value.zeroValue(type);
        this.timestamp = 0;
        this.height = 0;
        this.confirmations = 0;
        this.isQueuedOutgoing = false;
        this.inputs = new ArrayList<>();
        this.outputs = new ArrayList<>();
        this.confirmationRiskProfile = null;
        this.rawSize = 0;
        this.fee = Value.zeroValue(type);
    }

    public BtcTransaction(CryptoCurrency type, Transaction transaction,
                          long transferred, int timestamp, int height, int confirmations,
                          boolean isQueuedOutgoing, ArrayList<GenericInput> inputs,
                          ArrayList<GenericOutput> outputs, ConfirmationRiskProfileLocal risk,
                          int rawSize, @Nullable Value fee) {
        this.type = type;
        this.tx = transaction;
        this.hash = tx.getId();
        this.transferred = Value.valueOf(type, transferred);
        this.timestamp = timestamp;
        this.confirmations = confirmations;
        this.height = height;
        this.isQueuedOutgoing = isQueuedOutgoing;
        this.inputs = inputs;
        this.outputs = outputs;
        this.confirmationRiskProfile = Optional.fromNullable(risk);
        this.rawSize = rawSize;
        this.fee = fee;
    }

    public BtcTransaction(){}

    @Override
    public boolean isQueuedOutgoing() {
        return isQueuedOutgoing;
    }

    @Override
    public GenericAssetInfo getType() {
        return type;
    }

    @Override
    public int getConfirmations() {
        return confirmations;
    }

    @Override
    public int getHeight() {
        return height;
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
    public Value getTransferred() {
        return transferred;
    }

    @Override
    public boolean isIncoming() {
        return transferred.value >= 0;
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
