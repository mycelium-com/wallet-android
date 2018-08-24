package com.mycelium.wapi.wallet.btc;

import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.GenericTransaction;
import com.mycelium.wapi.wallet.coins.BitcoinMain;
import com.mycelium.wapi.wallet.coins.CoinType;
import com.mycelium.wapi.wallet.coins.Value;

import javax.annotation.Nullable;
import java.util.List;

public class BtcTransaction implements GenericTransaction {
    final CoinType type;
    final Sha256Hash hash;
    final Transaction tx;
    final Value valueSent;
    final Value valueReceived;
    final Value value;
    @Nullable
    final Value fee;

    public BtcTransaction(CoinType type, Sha256Hash transactionId, Transaction transaction,
                          Value valueSent, Value valueReceived, @Nullable Value fee) {
        this.type = type;
        this.tx = transaction;
        this.hash = transactionId;
        this.valueSent = valueSent;
        this.valueReceived = valueReceived;
        this.value = valueReceived.subtract(valueSent);
        this.fee = fee;
    }

    public BtcTransaction(CoinType type, Transaction transaction) {
        this(type, transaction.getHash(), transaction, null, null, null);
    }

    @Override
    public CoinType getType() {
        return type;
    }

    @Override
    public int getAppearedAtChainHeight() {
        return 0;
    }

    @Override
    public void setAppearedAtChainHeight(int appearedAtChainHeight) {
    }

    @Override
    public int getDepthInBlocks() {
        return 0;
    }

    @Override
    public void setDepthInBlocks(int depthInBlocks) {

    }

    @Override
    public long getTimestamp() {
        return 0;
    }

    @Override
    public void setTimestamp(long timestamp) {

    }


    @Override
    @Nullable
    public Value getFee() {
        return fee;
    }

    @Override
    public List<GenericAddress> getReceivedFrom() {
        return null;
    }

    @Override
    public List<GenericOutput> getSentTo() {
        return null;
    }

    @Override
    public Value getSent() {
        return Value.valueOf(BitcoinMain.get(),5);
    }

    @Override
    public Value getReceived() {
        return Value.valueOf(BitcoinMain.get(),10);
    }

    @Override
    public boolean isIncoming() {
        return false;
    }


    @Override
    public Sha256Hash getHash() {
        return tx.getHash();
    }

    @Override
    public String getHashAsString() {
        return getHash().toString();
    }

    @Override
    public byte[] getHashBytes() {
        return getHash().getBytes();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BtcTransaction other = (BtcTransaction) o;
        return getHash().equals(other.getHash());
    }

    public Transaction getRawTransaction() {
        return tx;
    }
}
