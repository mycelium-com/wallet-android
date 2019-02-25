package com.mycelium;

import com.mrd.bitlib.model.OutPoint;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.model.TransactionEx;
import com.mycelium.wapi.model.TransactionOutputEx;
import com.mycelium.wapi.wallet.AccountBacking;
import com.mycelium.wapi.wallet.FeeEstimationsGeneric;
import com.mycelium.wapi.wallet.colu.ColuTransaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ColuAccountBacking implements AccountBacking<ColuTransaction> {
    @Override
    public void beginTransaction() {

    }

    @Override
    public void setTransactionSuccessful() {

    }

    @Override
    public void endTransaction() {

    }

    @Override
    public void clear() {

    }

    @Override
    public Collection<TransactionOutputEx> getAllUnspentOutputs() {
        return null;
    }

    @Override
    public TransactionOutputEx getUnspentOutput(OutPoint outPoint) {
        return null;
    }

    @Override
    public void deleteUnspentOutput(OutPoint outPoint) {

    }

    @Override
    public void putUnspentOutput(TransactionOutputEx output) {

    }

    @Override
    public void putParentTransactionOuputs(List<TransactionOutputEx> outputsList) {

    }

    @Override
    public void putParentTransactionOutput(TransactionOutputEx output) {

    }

    @Override
    public TransactionOutputEx getParentTransactionOutput(OutPoint outPoint) {
        return null;
    }

    @Override
    public boolean hasParentTransactionOutput(OutPoint outPoint) {
        return false;
    }

    @Override
    public void putTransaction(TransactionEx transaction) {

    }

    @Override
    public void putTransactions(Collection<? extends TransactionEx> transactions) {

    }

    @Override
    public TransactionEx getTransaction(Sha256Hash hash) {
        return null;
    }

    @Override
    public void deleteTransaction(Sha256Hash hash) {

    }

    @Override
    public List<TransactionEx> getTransactionHistory(int offset, int limit) {
        return null;
    }

    @Override
    public List<TransactionEx> getTransactionsSince(long since) {
        return null;
    }

    @Override
    public Collection<TransactionEx> getUnconfirmedTransactions() {
        return null;
    }

    @Override
    public Collection<TransactionEx> getYoungTransactions(int maxConfirmations, int blockChainHeight) {
        return null;
    }

    @Override
    public boolean hasTransaction(Sha256Hash txid) {
        return false;
    }

    @Override
    public void putOutgoingTransaction(Sha256Hash txid, byte[] rawTransaction) {

    }

    @Override
    public Map<Sha256Hash, byte[]> getOutgoingTransactions() {
        return null;
    }

    @Override
    public boolean isOutgoingTransaction(Sha256Hash txid) {
        return false;
    }

    @Override
    public void removeOutgoingTransaction(Sha256Hash txid) {

    }

    @Override
    public void deleteTxRefersParentTransaction(Sha256Hash txId) {

    }

    @Override
    public Collection<Sha256Hash> getTransactionsReferencingOutPoint(OutPoint outPoint) {
        return null;
    }

    @Override
    public void putTxRefersParentTransaction(Sha256Hash txId, List<OutPoint> refersOutputs) {

    }

    private List<ColuTransaction> transactions = new ArrayList<>();

    @Override
    public ColuTransaction getTx(Sha256Hash hash) {
        return null;
    }


    @Override
    public List<ColuTransaction> getTransactions(int offset, int limit) {
        return transactions;
    }

    @Override
    public void putTransactions(List<ColuTransaction> txList) {
        transactions.addAll(txList);

    }

    @Override
    public void putFeeEstimation(FeeEstimationsGeneric feeEstimation) {

    }
}
