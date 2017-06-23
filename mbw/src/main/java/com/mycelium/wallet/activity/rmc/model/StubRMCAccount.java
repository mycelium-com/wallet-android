package com.mycelium.wallet.activity.rmc.model;

import com.google.common.base.Optional;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.OutputList;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.model.Balance;
import com.mycelium.wapi.model.TransactionDetails;
import com.mycelium.wapi.model.TransactionEx;
import com.mycelium.wapi.model.TransactionOutputSummary;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Created by elvis on 23.06.17.
 */

public class StubRMCAccount implements WalletAccount {
    @Override
    public void checkAmount(Receiver receiver, long kbMinerFee, CurrencyValue enteredAmount) throws StandardTransactionBuilder.InsufficientFundsException, StandardTransactionBuilder.OutputTooSmallException, StandardTransactionBuilder.UnableToBuildTransactionException {

    }

    @Override
    public NetworkParameters getNetwork() {
        return null;
    }

    @Override
    public boolean isMine(Address address) {
        return false;
    }

    @Override
    public boolean synchronize(SyncMode mode) {
        return false;
    }

    @Override
    public UUID getId() {
        return UUID.randomUUID();
    }

    @Override
    public void setAllowZeroConfSpending(boolean allowZeroConfSpending) {

    }

    @Override
    public int getBlockChainHeight() {
        return 0;
    }

    @Override
    public Optional<Address> getReceivingAddress() {
        return Optional.of(new Address(new byte[0], "RMC1237LKDSAJFLK2323234hkj2#JH$KJ#H$KJH#$KJ#@H$KJ"));
    }

    @Override
    public boolean canSpend() {
        return true;
    }

    @Override
    public Balance getBalance() {
        return new Balance(20000000, 2000000, 1000000, 0, 0, 10000, true, false);
    }

    @Override
    public CurrencyBasedBalance getCurrencyBasedBalance() {
        ExactCurrencyValue balance = new ExactCurrencyValue() {
            @Override
            public String getCurrency() {
                return "RMC";
            }

            @Override
            public BigDecimal getValue() {
                return new BigDecimal(20);
            }
        };
        return new CurrencyBasedBalance(balance, balance, balance);
    }

    @Override
    public List<TransactionSummary> getTransactionHistory(int offset, int limit) {
        return null;
    }

    @Override
    public List<TransactionSummary> getTransactionsSince(Long receivingSince) {
        return null;
    }

    @Override
    public TransactionSummary getTransactionSummary(Sha256Hash txid) {
        return null;
    }

    @Override
    public TransactionDetails getTransactionDetails(Sha256Hash txid) {
        return null;
    }

    @Override
    public boolean isArchived() {
        return false;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void archiveAccount() {

    }

    @Override
    public void activateAccount() {

    }

    @Override
    public void dropCachedData() {

    }

    @Override
    public StandardTransactionBuilder.UnsignedTransaction createUnsignedTransaction(List<Receiver> receivers, long minerFeeToUse) throws StandardTransactionBuilder.OutputTooSmallException, StandardTransactionBuilder.InsufficientFundsException, StandardTransactionBuilder.UnableToBuildTransactionException {
        return null;
    }

    @Override
    public StandardTransactionBuilder.UnsignedTransaction createUnsignedTransaction(OutputList outputs, long minerFeeToUse) throws StandardTransactionBuilder.OutputTooSmallException, StandardTransactionBuilder.InsufficientFundsException, StandardTransactionBuilder.UnableToBuildTransactionException {
        return null;
    }

    @Override
    public Transaction signTransaction(StandardTransactionBuilder.UnsignedTransaction unsigned, KeyCipher cipher) throws KeyCipher.InvalidKeyCipher {
        return null;
    }

    @Override
    public boolean broadcastOutgoingTransactions() {
        return false;
    }

    @Override
    public BroadcastResult broadcastTransaction(Transaction transaction) {
        return null;
    }

    @Override
    public TransactionEx getTransaction(Sha256Hash txid) {
        return null;
    }

    @Override
    public void queueTransaction(TransactionEx transaction) {

    }

    @Override
    public boolean cancelQueuedTransaction(Sha256Hash transactionId) {
        return false;
    }

    @Override
    public boolean deleteTransaction(Sha256Hash transactionId) {
        return false;
    }

    @Override
    public ExactCurrencyValue calculateMaxSpendableAmount(long minerFeeToUse) {
        return null;
    }

    @Override
    public boolean isValidEncryptionKey(KeyCipher cipher) {
        return false;
    }

    @Override
    public boolean isDerivedFromInternalMasterseed() {
        return false;
    }

    @Override
    public boolean isOwnInternalAddress(Address address) {
        return false;
    }

    @Override
    public StandardTransactionBuilder.UnsignedTransaction createUnsignedPop(Sha256Hash txid, byte[] nonce) {
        return null;
    }

    @Override
    public boolean isOwnExternalAddress(Address address) {
        return false;
    }

    @Override
    public List<TransactionOutputSummary> getUnspentTransactionOutputSummary() {
        return null;
    }

    @Override
    public boolean onlySyncWhenActive() {
        return false;
    }

    @Override
    public String getAccountDefaultCurrency() {
        return null;
    }
}
