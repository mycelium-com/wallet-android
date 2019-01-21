package com.mycelium.wapi.wallet.eth;

import com.google.common.base.Optional;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.wallet.ConfirmationRiskProfileLocal;
import com.mycelium.wapi.wallet.GenericTransaction;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.eth.coins.EthMain;

import java.util.List;

public class EthTransaction implements GenericTransaction {

    private List<GenericInput> inputs;
    private List<GenericOutput> outputs;
    private Value sentValue;

    EthTransaction(Value sentValue, List<GenericInput> inputs, List<GenericOutput> outputs) {
        this.sentValue = sentValue;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    @Override
    public CryptoCurrency getType() {
        return null;
    }

    @Override
    public Sha256Hash getHash() {
        return Sha256Hash.ZERO_HASH;
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
        return Optional.absent();
    }

    public void setTimestamp(long timestamp) {

    }

    @Override
    public Value getFee() {
        return null;
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
        return sentValue;
    }

    @Override
    public Value getReceived() {
        return null;
    }

    @Override
    public Value getTransferred() {
        return null;
    }

    @Override
    public boolean isIncoming() {
        return false;
    }

    @Override
    public int getRawSize() {
        return 0;
    }

}
