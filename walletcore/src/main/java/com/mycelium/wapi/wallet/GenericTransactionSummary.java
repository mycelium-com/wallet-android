package com.mycelium.wapi.wallet;

import com.google.common.base.Optional;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.GenericAssetInfo;
import com.mycelium.wapi.wallet.coins.Value;

import java.io.Serializable;
import java.util.List;

import javax.annotation.Nullable;

public class GenericTransactionSummary implements Serializable {

    protected CryptoCurrency type;
    protected byte[] id;
    protected byte[] hash;
    protected Value transferred;
    protected long timestamp;
    protected GenericAddress destinationAddress;
    protected List<GenericInputViewModel> inputs;
    protected List<GenericOutputViewModel> outputs;
    protected int height;
    protected int confirmations;
    protected int rawSize;
    protected boolean isQueuedOutgoing;
    protected long time;
    transient public Optional<ConfirmationRiskProfileLocal> confirmationRiskProfile;
    @Nullable
    protected Value fee;


    public GenericTransactionSummary(CryptoCurrency type,
                                     byte[] id, byte[] hash,
                                     Value transferred,
                                     long timestamp,
                                     int height,
                                     int confirmations,
                                     boolean isQueuedOutgoing,
                                     GenericAddress destinationAddress,
                                     List<GenericInputViewModel> inputs,
                                     List<GenericOutputViewModel> outputs,
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

    public List<GenericInputViewModel> getInputs() {
        return inputs;
    }

    public List<GenericOutputViewModel> getOutputs() {
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

    public byte[] getId() {
        return id;
    }

    public long getTime() {return time;}

    public Optional<ConfirmationRiskProfileLocal> getConfirmationRiskProfile() {
        return confirmationRiskProfile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenericTransactionSummary other = (GenericTransactionSummary) o;
        return getId().equals(other.getId());
    }

    @Override
    public String toString(){
        return HexUtils.toHex(hash);
    }
}
