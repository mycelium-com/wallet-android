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
import com.mycelium.wapi.wallet.AddressUtils;
import com.mycelium.wapi.wallet.BroadcastResult;
import com.mycelium.wapi.wallet.FeeEstimationsGeneric;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.GenericTransaction;
import com.mycelium.wapi.wallet.SendRequest;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.btc.WalletBtcAccount;
import com.mycelium.wapi.wallet.coins.Balance;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.exceptions.TransactionBroadcastException;
import com.mycelium.wapi.wallet.eth.coins.*;

import net.bytebuddy.utility.RandomString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class EthAccount implements WalletAccount<EthTransaction, EthAddress> {

    private static HashMap<String, Value> accountBalancesStorage = new HashMap<>();
    private static List<EthTransaction> transactionStorage = new ArrayList<>();

    private UUID id;
    private EthAddress address;

    public EthAccount() {
        id = UUID.randomUUID();
        address = new EthAddress(RandomString.make(20));
        accountBalancesStorage.put(address.toString(), Value.valueOf(EthMain.INSTANCE, 1253120));
    }

    public UUID getId(){
        return id;
    }

    @Override
    public void setAllowZeroConfSpending(boolean b) {

    }

    @Override
    public void completeAndSignTx(SendRequest<EthTransaction> request) throws WalletAccountException {

    }

    @Override
    public void completeTransaction(SendRequest<EthTransaction> request) throws WalletAccountException {
        EthSendRequest sendRequest = (EthSendRequest)request;
        Balance balance = getAccountBalance();
        List<GenericTransaction.GenericInput> inputs = new ArrayList<>(Arrays.asList(new GenericTransaction.GenericInput(this.address, balance.confirmed)));
        List<GenericTransaction.GenericOutput> outputs = new ArrayList<>(Arrays.asList(new GenericTransaction.GenericOutput(sendRequest.getDestination(), balance.confirmed.subtract(sendRequest.getAmount()))));
        sendRequest.tx = new EthTransaction(sendRequest.getAmount(), inputs, outputs);
    }

    @Override
    public void signTransaction(SendRequest<EthTransaction> request) throws WalletAccountException {

    }


    @Override
    public BroadcastResult broadcastTx(EthTransaction tx) throws TransactionBroadcastException {
        EthAddress from = (EthAddress)tx.getInputs().get(0).getAddress();
        EthAddress to = (EthAddress)tx.getOutputs().get(0).getAddress();
        tx.getSent();

        Value fromAccountInitialBalance = accountBalancesStorage.get(from.toString());
        Value toAccountInitialBalance = accountBalancesStorage.get(to.toString());

        accountBalancesStorage.put(from.toString(),fromAccountInitialBalance.subtract(tx.getSent()));
        accountBalancesStorage.put(to.toString(), toAccountInitialBalance.add(tx.getSent()));

        transactionStorage.add(tx);

        return null;
    }

    @Override
    public GenericAddress getReceiveAddress() {
        return address;
    }

    @Override
    public CryptoCurrency getCoinType() {
        return EthMain.INSTANCE;
    }

    @Override
    public Balance getAccountBalance() {
        Value balance = accountBalancesStorage.get(address.toString());
        return new Balance(balance, Value.zeroValue(getCoinType()),Value.zeroValue(getCoinType()),Value.zeroValue(getCoinType()));
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
        return transactionStorage;
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
    public boolean isSynchronizing() {
        return false;
    }

    @Override
    public boolean broadcastOutgoingTransactions() {
        return false;
    }

    @Override
    public Value calculateMaxSpendableAmount(long minerFeeToUse) {
        return null;
    }

    @Override
    public int getSyncTotalRetrievedTransactions() {
        return 0;
    }

    @Override
    public FeeEstimationsGeneric getFeeEstimations() {
        return null;
    }

    @Override
    public SendRequest getSendToRequest(EthAddress destination, Value amount) {
        return EthSendRequest.to(destination, amount);
    }

}
