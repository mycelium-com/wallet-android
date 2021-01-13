package com.mycelium.wapi.wallet;

import com.google.common.base.Optional;
import com.mrd.bitlib.util.HexUtils;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.AssetInfo;
import com.mycelium.wapi.wallet.coins.Value;

import org.jetbrains.annotations.NotNull;
import java.io.Serializable;
import java.util.List;

import javax.annotation.Nullable;

public class TransactionSummary implements Serializable, Comparable<TransactionSummary> {

    protected CryptoCurrency type;
    protected byte[] id;
    protected byte[] hash;
    protected Value transferred;
    protected long timestamp;
    protected List<InputViewModel> inputs;
    protected List<OutputViewModel> outputs;
    protected List<Address> destinationAddresses;
    protected int height;
    protected int confirmations;
    protected int rawSize;
    protected boolean isQueuedOutgoing;
    transient public Optional<ConfirmationRiskProfileLocal> confirmationRiskProfile;
    @Nullable
    protected Value fee;


    public TransactionSummary(CryptoCurrency type,
                                     byte[] id, byte[] hash,
                                     Value transferred,
                                     long timestamp,
                                     int height,
                                     int confirmations,
                                     boolean isQueuedOutgoing,
                              List<InputViewModel> inputs,
                              List<OutputViewModel> outputs,
                              List<Address> destinationAddresses,
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
        this.inputs = inputs;
        this.outputs = outputs;
        this.destinationAddresses = destinationAddresses;
        this.confirmationRiskProfile = Optional.fromNullable(risk);
        this.rawSize = rawSize;
        this.fee = fee;
    }

    public boolean isQueuedOutgoing() {
        return isQueuedOutgoing;
    }

    public AssetInfo getType() {
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

    public List<InputViewModel> getInputs() {
        return inputs;
    }

    public List<OutputViewModel> getOutputs() {
        return outputs;
    }

    public List<Address> getDestinationAddresses() {
        return destinationAddresses;
    }

    public Value getTransferred() {
        return transferred;
    }

    public boolean isIncoming() {
        return transferred.moreOrEqualThanZero();
    }

    public int getRawSize() {
        return rawSize;
    }

    public byte[] getId() {
        return id;
    }

    public String getIdHex() {
        return HexUtils.toHex(id);
    }

    public Optional<ConfirmationRiskProfileLocal> getConfirmationRiskProfile() {
        return confirmationRiskProfile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionSummary other = (TransactionSummary) o;
        return getId().equals(other.getId());
    }

    @Override
    public String toString(){
        return getIdHex();
    }

    public boolean canCancel() {
        return isQueuedOutgoing;
    }

    @Override
    public int compareTo(@NotNull TransactionSummary other) {
        // TODO: Fix block heights! Currently the block heights are calculated as latest block height - confirmations + 1 but as it's not atomically collecting all the data, we run off by one frequently for transactions that get synced during a block being discovered.

        // Blockchains core property is that they determine the sorting of transactions.
        // In Bitcoin, timestamps of transactions are not required to be increasing, so the
        // block height is what has to be sorted by for transactions from different blocks.
        // if (other.getHeight() != getHeight()) {
        //     return other.getHeight() - getHeight();
        // }

        // If no block height is available (alt coins?), we do sort by timestamp.
        if (other.getTimestamp() != getTimestamp()) {
            return (int) (other.getTimestamp() - getTimestamp());
        }
        // Transactions are sorted within a block, too. Here we don't have that sequence number
        // handy but to ensure stable sorting, we have to sort by something robust.
        return getIdHex().compareTo(other.getIdHex());
    }
}
