package com.mycelium.wapi.wallet.fiat;

import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.wallet.BroadcastResult;
import com.mycelium.wapi.wallet.FeeEstimationsGeneric;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.GenericTransaction;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.SendRequest;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.coins.Balance;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.exceptions.GenericBuildTransactionException;
import com.mycelium.wapi.wallet.exceptions.GenericInsufficientFundsException;
import com.mycelium.wapi.wallet.exceptions.GenericOutputTooSmallException;
import com.mycelium.wapi.wallet.exceptions.GenericTransactionBroadcastException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FiatAccount implements WalletAccount<FiatTransaction, FiatAddress> {
    @Override
    public void setAllowZeroConfSpending(boolean b) {

    }

    @Override
    public void completeTransaction(SendRequest<FiatTransaction> request) throws GenericBuildTransactionException, GenericInsufficientFundsException, GenericOutputTooSmallException {

    }

    @Override
    public void signTransaction(SendRequest<FiatTransaction> request, KeyCipher keyCipher) throws KeyCipher.InvalidKeyCipher {

    }


    @Override
    public BroadcastResult broadcastTx(FiatTransaction tx) throws GenericTransactionBroadcastException {
        return null;
    }

    @Override
    public GenericAddress getReceiveAddress() {
        return null;
    }

    @Override
    public CryptoCurrency getCoinType() {
        return null;
    }

    @Override
    public Balance getAccountBalance() {
        return null;
    }

    @Override
    public boolean isMineAddress(GenericAddress address) {
        return false;
    }

    @Override
    public boolean isExchangeable() {
        return false;
    }

    @Override
    public FiatTransaction getTx(Sha256Hash transactionId) {
        return null;
    }

    @Override
    public List<FiatTransaction> getTransactions(int offset, int limit) {
        return null;
    }

    @Override
    public List<FiatTransaction> getTransactionsSince(long receivingSince) {
        return new ArrayList<>();
    }

    @Override
    public SendRequest<FiatTransaction> getSendToRequest(FiatAddress destination, Value amount, Value fee) {
        return null;
    }

    @Override
    public boolean synchronize(SyncMode mode) {
        return false;
    }

    @Override
    public int getBlockChainHeight() {
        return 0;
    }

    @Override
    public boolean canSpend() {
        return false;
    }

    @Override
    public boolean isArchived() {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
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
    public boolean isVisible() {
        return false;
    }

    @Override
    public boolean isDerivedFromInternalMasterseed() {
        return false;
    }

    @Override
    public UUID getId() {
        return null;
    }

    @Override
    public boolean isSynchronizing() {
        return false;
    }

    @Override
    public boolean broadcastOutgoingTransactions() {
        return false;
    }

    @Override
    public void removeAllQueuedTransactions() {
    }

    @Override
    public Value calculateMaxSpendableAmount(long minerFeeToUse, FiatAddress destinationAddress) {
        return null;
    }

    @Override
    public int getSyncTotalRetrievedTransactions() {
        return 0;
    }

    @Override
    public FeeEstimationsGeneric getFeeEstimations() {
        return new FeeEstimationsGeneric(Value.valueOf(getCoinType(), 1000), Value.valueOf(getCoinType(), 1000), Value.valueOf(getCoinType(), 1000),Value.valueOf(getCoinType(), 1000));
    }

    @Override
    public int getTypicalEstimatedTransactionSize() {
        return 0;
    }

    @Override
    public InMemoryPrivateKey getPrivateKey(KeyCipher cipher) throws KeyCipher.InvalidKeyCipher {
        return null;
    }

    @Override
    public FiatAddress getDummyAddress() {
        return null;
    }


    @Override
    public List<GenericTransaction.GenericOutput> getUnspentOutputs() {
        return new ArrayList<>();
    }

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public void setLabel(String label) {

    }
}
