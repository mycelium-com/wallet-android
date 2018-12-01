package com.mycelium.wapi.wallet.bch.bip44;

import com.mrd.bitlib.model.Transaction;
import com.mycelium.wapi.wallet.ConfirmationRiskProfileLocal;
import com.mycelium.wapi.wallet.btc.BtcTransaction;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.Value;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class BchTransaction extends BtcTransaction {

    public BchTransaction(CryptoCurrency type, Transaction transaction) {
        super(type, transaction);
    }

    public BchTransaction(CryptoCurrency type, Transaction transaction,
                          long valueSent, long valueReceived, int timestamp, int confirmations,
                          boolean isQueuedOutgoing, ArrayList<GenericInput> inputs,
                          ArrayList<GenericOutput> outputs, ConfirmationRiskProfileLocal risk,
                          int rawSize, @Nullable Value fee) {
        super(type, transaction, valueSent, valueReceived, timestamp, confirmations,
                isQueuedOutgoing, inputs, outputs, risk, rawSize, fee);
    }

}