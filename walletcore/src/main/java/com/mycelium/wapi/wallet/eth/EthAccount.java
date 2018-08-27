package com.mycelium.wapi.wallet.eth;
;
import com.mycelium.wapi.wallet.SendRequest;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.WalletAccount;;
import com.mycelium.wapi.wallet.coins.Balance;
import com.mycelium.wapi.wallet.coins.CoinType;
import com.mycelium.wapi.wallet.exceptions.TransactionBroadcastException;;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EthAccount implements WalletAccount<EthTransaction, EthAddress> {

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
    public void broadcastTx(EthTransaction tx) throws TransactionBroadcastException {

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
    public EthTransaction getTransaction(String transactionId) {
        return null;
    }

    @Override
    public List<EthTransaction> getTransactions(int offset, int limit) {
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
}
