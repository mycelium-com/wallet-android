package com.mycelium.wapi.wallet;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.model.IssuedKeysInfo;
import com.mycelium.wapi.model.TransactionDetails;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;

import java.util.List;

public interface SpvBalanceFetcher {
    CurrencyBasedBalance retrieveByHdAccountIndex(String id, int accountIndex);
    CurrencyBasedBalance retrieveByUnrelatedAccountId(String id);
    List<TransactionSummary> retrieveTransactionsSummaryByHdAccountIndex(String id, int accountIndex);
    List<TransactionSummary> retrieveTransactionsSummaryByHdAccountIndex(String id, int accountIndex, long since);
    List<TransactionSummary> retrieveTransactionsSummaryByHdAccountIndex(String id, int accountIndex, int offset, int limit);
    List<TransactionSummary> retrieveTransactionsSummaryByUnrelatedAccountId(String id);
    List<TransactionSummary> retrieveTransactionsSummaryByUnrelatedAccountId(String id, long since);
    List<TransactionSummary> retrieveTransactionsSummaryByUnrelatedAccountId(String id, int offset, int limit);

    TransactionDetails retrieveTransactionDetails(Sha256Hash txid);
    void requestTransactionsAsync(int accountIndex);
    void requestHdWalletAccountRemoval(int accountIndex);
    void requestTransactionsFromUnrelatedAccountAsync(String guid, int accountType);
    void requestUnrelatedAccountRemoval(String guid);
    float getSyncProgressPercents();
    Address getCurrentReceiveAddress(int accountIndex);
    Address getCurrentReceiveAddressUnrelated(String guid);
    IssuedKeysInfo getPrivateKeysCount(int accountIndex);
    IssuedKeysInfo getPrivateKeysCountUnrelated(String guid);
    boolean isAccountSynced(WalletAccount account);
    boolean isAccountVisible(WalletAccount account);
    void setVisible(WalletAccount account);
    void forceCleanCache();
    long calculateMaxSpendableAmount(int accountIndex, String txFee, float txFeeFactor);
    long calculateMaxSpendableAmountUnrelatedAccount(String guid, String txFee, float txFeeFactor);
    long getMaxFundsTransferrable(int accountIndex);
    long getMaxFundsTransferrableUnrelatedAccount(String guid);
    long estimateFeeFromTransferrableAmount(int accountIndex, long amountSatoshis, String txFee, float txFeeFactor);
    long estimateFeeFromTransferrableAmountUnrelatedAccount(String guid, long amountSatoshis, String txFee, float txFeeFactor);
}
