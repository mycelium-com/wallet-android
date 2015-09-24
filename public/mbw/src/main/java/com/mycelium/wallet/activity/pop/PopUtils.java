package com.mycelium.wallet.activity.pop;

import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wallet.pop.PopRequest;
import com.mycelium.wapi.model.TransactionSummary;

public class PopUtils {
    public static boolean matches(PopRequest popRequest, MetadataStorage metadataStorage, TransactionSummary transactionSummary) {
        if (popRequest.getTxid() != null && !transactionSummary.txid.equals(popRequest.getTxid())) {
            return false;
        }
        Long amountSatoshis = popRequest.getAmountSatoshis();
        if (amountSatoshis != null && amountSatoshis != -transactionSummary.value) {
            return false;
        }
        if (popRequest.getLabel() != null) {
            String label = metadataStorage.getLabelByTransaction(transactionSummary.txid);
            if (!popRequest.getLabel().equals(label)) {
                return false;
            }
        }
        return true;
    }
}
