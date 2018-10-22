package com.mycelium.wapi.wallet.eth;

import com.google.common.base.Optional;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.UnsignedTransaction;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.model.TransactionEx;
import com.mycelium.wapi.model.TransactionOutputSummary;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.BroadcastResult;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.GenericTransaction;
import com.mycelium.wapi.wallet.SendRequest;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.btc.WalletBtcAccount;
import com.mycelium.wapi.wallet.coins.Balance;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;
import com.mycelium.wapi.wallet.exceptions.TransactionBroadcastException;;

import java.util.List;
import java.util.UUID;

public class EthAccount implements WalletAccount<EthTransaction, EthAddress> {

    @Override
    public void setAllowZeroConfSpending(boolean b) {

    }

    @Override
    public void completeAndSignTx(SendRequest<EthTransaction> request) throws WalletAccountException {

    }

    @Override
    public void completeTransaction(SendRequest<EthTransaction> request) throws WalletAccountException {

    }

    @Override
    public void signTransaction(SendRequest<EthTransaction> request) throws WalletAccountException {

    }

    @Override
    public BroadcastResult broadcastTx(EthTransaction tx) throws TransactionBroadcastException {
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
    public EthTransaction getTx(Sha256Hash transactionId) {
        return null;
    }

    @Override
    public List<EthTransaction> getTransactions(int offset, int limit) {
        return null;
    }

    @Override
    public void checkAmount(WalletAccount.Receiver receiver, long kbMinerFee, Value enteredAmount) throws StandardTransactionBuilder.InsufficientFundsException, StandardTransactionBuilder.OutputTooSmallException, StandardTransactionBuilder.UnableToBuildTransactionException {

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
    public int getSyncTotalRetrievedTransactions() {
        return 0;
    }

    @Override
    public SendRequest getSendToRequest(EthAddress destination, Value amount) {
        return null;
    }

}
