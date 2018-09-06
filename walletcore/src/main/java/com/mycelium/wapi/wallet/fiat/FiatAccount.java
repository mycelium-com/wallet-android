package com.mycelium.wapi.wallet.fiat;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.GenericTransaction;
import com.mycelium.wapi.wallet.SendRequest;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.btc.BtcSendRequest;
import com.mycelium.wapi.wallet.coins.Balance;
import com.mycelium.wapi.wallet.coins.CoinType;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.exceptions.TransactionBroadcastException;

import java.util.List;
import java.util.UUID;

public class FiatAccount implements WalletAccount<FiatTransaction, FiatAddress> {
    @Override
    public void completeAndSignTx(SendRequest<FiatTransaction> request) throws WalletAccountException {

    }

    @Override
    public void completeTransaction(SendRequest<FiatTransaction> request) throws WalletAccountException {

    }

    @Override
    public void signTransaction(SendRequest<FiatTransaction> request) throws WalletAccountException {

    }

    @Override
    public void broadcastTx(FiatTransaction tx) throws TransactionBroadcastException {

    }

    @Override
    public CoinType getCoinType() {
        return null;
    }

    @Override
    public Balance getAccountBalance() {
        return null;
    }

    @Override
    public FiatTransaction getTransaction(Sha256Hash transactionId) {
        return null;
    }

    @Override
    public List<GenericTransaction> getTransactions(int offset, int limit) {
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
    public boolean isMine(Address address) {
        return false;
    }

    @Override
    public SendRequest getSendToRequest(GenericAddress destination, Value amount) {
        return null;
    }
}
