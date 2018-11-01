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

    EthTransaction(List<GenericInput> inputs, List<GenericOutput> outputs) {
        this.sentValue = Value.zeroValue(EthMain.INSTANCE);
        for (GenericInput input : inputs) {
            this.sentValue.add(input.getValue());
        }

        for (GenericOutput output : outputs) {
            this.sentValue.subtract(output.getValue());
        }
    }

    @Override
    public CryptoCurrency getType() {
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
    public boolean isIncoming() {
        return false;
    }

    @Override
    public int getRawSize() {
        return 0;
    }

}
