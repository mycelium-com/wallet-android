package com.mycelium.wapi.wallet;

import com.google.common.base.Optional;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.AbstractAsset;
import com.mycelium.wapi.wallet.coins.GenericAssetInfo;
import com.mycelium.wapi.wallet.coins.Value;

import java.io.Serializable;
import java.util.List;

public interface GenericTransaction extends Serializable {
    class GenericOutput implements Serializable {
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

    class GenericInput extends GenericOutput{
        public GenericInput(GenericAddress genericAddress, Value value) {
            super(genericAddress, value);
        }
    }

    GenericAssetInfo getType();

    Sha256Hash getHash();
    String getHashAsString();
    byte[] getHashBytes();

    int getHeight();

    int getConfirmations();

    long getTimestamp();
    void setTimestamp(long timestamp);

    boolean isQueuedOutgoing();
    Optional<ConfirmationRiskProfileLocal> getConfirmationRiskProfile();

    Value getFee();

    List<GenericInput> getInputs();
    List<GenericOutput> getOutputs();

    /**
     * Amount transferred to the corresponding account.
     * Can be positive (funds arrived to the account) or negative (funds are sent from the account)
     */
    Value getTransferred();

    boolean isIncoming();

    int getRawSize();
}
