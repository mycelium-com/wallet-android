package com.mycelium.wapi.wallet.eth;

import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.wallet.BroadcastResult;
import com.mycelium.wapi.wallet.BroadcastResultType;
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
import com.mycelium.wapi.wallet.eth.coins.EthMain;
import com.mycelium.wapi.wallet.exceptions.GenericBuildTransactionException;
import com.mycelium.wapi.wallet.exceptions.GenericInsufficientFundsException;
import com.mycelium.wapi.wallet.exceptions.GenericOutputTooSmallException;
import com.mycelium.wapi.wallet.exceptions.GenericTransactionBroadcastException;

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
    private String label;

    public EthAccount() {
        id = UUID.randomUUID();
        address = new EthAddress(RandomString.make(24));
        accountBalancesStorage.put(address.toString(), Value.valueOf(EthMain.INSTANCE, 1000000));
    }

    public UUID getId(){
        return id;
    }

    @Override
    public void setAllowZeroConfSpending(boolean b) {

    }

    @Override
    public void completeTransaction(SendRequest<EthTransaction> request) throws GenericBuildTransactionException, GenericInsufficientFundsException, GenericOutputTooSmallException {
        EthSendRequest sendRequest = (EthSendRequest)request;
        Balance balance = getAccountBalance();
        List<GenericTransaction.GenericInput> inputs = new ArrayList<>(Arrays.asList(new GenericTransaction.GenericInput(this.address, balance.confirmed)));
        List<GenericTransaction.GenericOutput> outputs = new ArrayList<>(Arrays.asList(new GenericTransaction.GenericOutput(sendRequest.getDestination(), balance.confirmed.subtract(sendRequest.getAmount()))));
        sendRequest.tx = new EthTransaction(sendRequest.getAmount(), inputs, outputs);
    }

    @Override
    public void signTransaction(SendRequest<EthTransaction> request, KeyCipher keyCipher) {

    }


    @Override
    public BroadcastResult broadcastTx(EthTransaction tx) throws GenericTransactionBroadcastException {
        EthAddress from = (EthAddress)tx.getInputs().get(0).getAddress();
        EthAddress to = (EthAddress)tx.getOutputs().get(0).getAddress();

        Value fromAccountInitialBalance = accountBalancesStorage.get(from.toString());
        Value toAccountInitialBalance = accountBalancesStorage.get(to.toString());

        accountBalancesStorage.put(from.toString(),fromAccountInitialBalance.subtract(tx.getTransferred()));
        accountBalancesStorage.put(to.toString(), toAccountInitialBalance.add(tx.getTransferred()));

        transactionStorage.add(tx);

        return new BroadcastResult(BroadcastResultType.SUCCESS);
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
    public boolean isExchangeable() {
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
    public List<EthTransaction> getTransactionsSince(long receivingSince) {
        return new ArrayList<>();
    }

    @Override
    public boolean synchronize(SyncMode mode) {
        return true;
    }

    @Override
    public int getBlockChainHeight() {
        return 0;
    }

    @Override
    public boolean canSpend() {
        return true;
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
    public boolean isVisible() {
        return true;
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
    public void removeAllQueuedTransactions() {
    }

    @Override
    public Value calculateMaxSpendableAmount(long minerFeeToUse, EthAddress destinationAddress) {
        return Value.zeroValue(EthMain.INSTANCE);
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
    public EthAddress getDummyAddress() {
        return new EthAddress("0x29D7d1dd5B6f9C864d9db560D72a247c178aE86B");
    }

    @Override
    public EthAddress getDummyAddress(String subType) {
        return getDummyAddress();
    }

    @Override
    public SendRequest getSendToRequest(EthAddress destination, Value amount, Value fee) {
        return EthSendRequest.to(destination, amount, fee);
    }

    @Override
    public List<GenericTransaction.GenericOutput> getUnspentOutputs() {
        return new ArrayList<>();
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
    }

}
