package com.mycelium.wapi.wallet.colu;

import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.model.TransactionOutputEx;
import com.mycelium.wapi.wallet.CommonAccountBacking;
import com.mycelium.wapi.wallet.GenericTransactionSummary;

import java.util.List;

/**
 * Interface ColuAccountBacking contains specific methods to retrieve and save Colu transactions
 * We store only generic information for Colu
 */
public interface ColuAccountBacking extends CommonAccountBacking {
    void putTransactions(List<GenericTransactionSummary> transactionSummaries);
    List<GenericTransactionSummary> getTransactionSummaries(int offset, int length);
    List<GenericTransactionSummary> getTransactionsSince(long receivingSince);
    GenericTransactionSummary getTxSummary(Sha256Hash txId);
    List<TransactionOutputEx> getUnspentOutputs();
    void putUnspentOutputs(List<TransactionOutputEx> unspentOutputs);
}


