package com.mycelium.wapi.wallet;

import com.mrd.bitlib.model.Address;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;

import java.util.List;
import java.util.UUID;

public interface SpvBalanceFetcher {
    CurrencyBasedBalance retrieveByHdAccountIndex(String id, int accountIndex);
    CurrencyBasedBalance retrieveByUnrelatedAccountId(String id);
    List<TransactionSummary> retrieveTransactionSummaryByHdAccountIndex(String id, int accountIndex);
    List<TransactionSummary> retrieveTransactionSummaryByHdAccountIndex(String id, int accountIndex, long since);
    List<TransactionSummary> retrieveTransactionSummaryByUnrelatedAccountId(String id);
    List<TransactionSummary> retrieveTransactionSummaryByUnrelatedAccountId(String id, long since);
    void requestTransactionsAsync(int accountIndex);
    void requestHdWalletAccountRemoval(int accountIndex);
    void requestTransactionsFromUnrelatedAccountAsync(String guid, int accountType);
    void requestUnrelatedAccountRemoval(String guid);
    float getSyncProgressPercents();
    Address getCurrentReceiveAddress(int accountIndex);
    Address getCurrentReceiveAddressUnrelated(String guid);
    int getPrivateKeysCount(int accountIndex);
    int getPrivateKeysCountUnrelated(String guid);
    boolean isFirstSync();
    void forceCleanCache();
    long calculateMaxSpendableAmount(int accountIndex, String txFee, float txFeeFactor);
    long calculateMaxSpendableAmountUnrelatedAccount(String guid, String txFee, float txFeeFactor);
    long getMaxFundsTransferrable(int accountIndex);
    long getMaxFundsTransferrableUnrelatedAccount(String guid);
    long estimateFeeFromTransferrableAmount(int accountIndex, long amountSatoshis, String txFee, float txFeeFactor);
    long estimateFeeFromTransferrableAmountUnrelatedAccount(String guid, long amountSatoshis, String txFee, float txFeeFactor);
    int getBlockchainHeight();
}
