package com.mycelium.wapi.wallet;

import com.mrd.bitlib.model.Address;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;

import java.util.List;

public interface SpvBalanceFetcher {
    CurrencyBasedBalance retrieveByHdAccountIndex(String id, int accountIndex);
    CurrencyBasedBalance retrieveBySingleAddressAccountId(String id);
    List<TransactionSummary> retrieveTransactionSummaryByHdAccountIndex(String id, int accountIndex);
    List<TransactionSummary> retrieveTransactionSummaryByHdAccountIndex(String id, int accountIndex, long since);
    List<TransactionSummary> retrieveTransactionSummaryBySingleAddressAccountId(String id);
    List<TransactionSummary> retrieveTransactionSummaryBySingleAddressAccountId(String id, long since);
    void requestTransactionsAsync(int accountIndex);
    void requestHdWalletAccountRemoval(int accountIndex);
    void requestTransactionsFromSingleAddressAccountAsync(String guid);
    void requestSingleAddressWalletAccountRemoval(String guid);
    float getSyncProgressPercents();
    Address getCurrentReceiveAddress(int accountIndex);
    int getPrivateKeysCount(int accountIndex);
    boolean isFirstSync();
    void forceCleanCache();
    long calculateMaxSpendableAmount(int accountIndex, String txFee, float txFeeFactor);
    long calculateMaxSpendableAmountSingleAddress(String guid, String txFee, float txFeeFactor);
    long getMaxFundsTransferrable(int accountIndex);
    long getMaxFundsTransferrableSingleAddress(String guid);
    long estimateFeeFromTransferrableAmount(int accountIndex, long amountSatoshis, String txFee, float txFeeFactor);
    long estimateFeeFromTransferrableAmountSingleAddress(String guid, long amountSatoshis, String txFee, float txFeeFactor);
}
