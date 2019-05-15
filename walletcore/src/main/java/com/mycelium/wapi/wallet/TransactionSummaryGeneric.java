package com.mycelium.wapi.wallet;

import com.google.common.base.Optional;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.GenericAssetInfo;
import com.mycelium.wapi.wallet.coins.Value;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class TransactionSummaryGeneric implements Serializable {

    protected CryptoCurrency type;
    protected Sha256Hash id;
    protected Sha256Hash hash;
    protected Value transferred;
    protected long timestamp;
    protected GenericAddress destinationAddress;
    protected List<GenericInput> inputs;
    protected List<GenericOutput> outputs;
    protected int height;
    protected int confirmations;
    protected int rawSize;
    protected boolean isQueuedOutgoing;
    protected long time;
    public  Optional<ConfirmationRiskProfileLocal> confirmationRiskProfile;
    @Nullable
    protected Value fee;


    public TransactionSummaryGeneric(CryptoCurrency type,
                                     Sha256Hash id, Sha256Hash hash,
                                     Value transferred,
                                     long timestamp,
                                     int height,
                                     int confirmations,
                                     boolean isQueuedOutgoing,
                                     GenericAddress destinationAddress,
                                     List<GenericInput> inputs,
                                     List<GenericOutput> outputs,
                                     ConfirmationRiskProfileLocal risk,
                                     int rawSize, @Nullable Value fee) {
        this.type = type;
        this.id = id;
        this.hash = hash;
        this.transferred = transferred;
        this.timestamp = timestamp;
        this.confirmations = confirmations;
        this.height = height;
        this.isQueuedOutgoing = isQueuedOutgoing;
        this.destinationAddress = destinationAddress;
        this.inputs = inputs;
        this.outputs = outputs;
        this.confirmationRiskProfile = Optional.fromNullable(risk);
        this.rawSize = rawSize;
        this.fee = fee;
    }

    public boolean isQueuedOutgoing() {
        return isQueuedOutgoing;
    }

    public GenericAssetInfo getType() {
        return type;
    }

    public int getConfirmations() {
        return confirmations;
    }

    public int getHeight() {
        return height;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Nullable
    public Value getFee() {
        return fee;
    }

    public List<GenericInput> getInputs() {
        return inputs;
    }

    public List<GenericOutput> getOutputs() {
        return outputs;
    }

    public GenericAddress getDestinationAddress() {
        return destinationAddress;
    }

    public Value getTransferred() {
        return transferred;
    }

    public boolean isIncoming() {
        return transferred.value >= 0;
    }

    public int getRawSize() {
        return rawSize;
    }

    public Sha256Hash getId() {
        return id;
    }

    public String getHashAsString() {
        return hash.toString();
    }

    public byte[] getHashBytes() {
        return hash.getBytes();
    }

    public byte[] getTxBytes() {
        return null;
    }

    public long getTime() {return time;}

    public Optional<ConfirmationRiskProfileLocal> getConfirmationRiskProfile() {
        return confirmationRiskProfile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionSummaryGeneric other = (TransactionSummaryGeneric) o;
        return getId().equals(other.getId());
    }

    @Override
    public String toString(){
        return hash.toString();
    }
}
