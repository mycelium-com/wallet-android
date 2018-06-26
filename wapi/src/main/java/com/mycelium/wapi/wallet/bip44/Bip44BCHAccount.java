package com.mycelium.wapi.wallet.bip44;

import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.model.IssuedKeysInfo;
import com.mycelium.wapi.model.TransactionDetails;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.Bip44AccountBacking;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.SpvBalanceFetcher;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinCashValue;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.mycelium.wapi.wallet.bip44.Bip44AccountContext.ACCOUNT_TYPE_FROM_MASTERSEED;

public class Bip44BCHAccount extends Bip44Account {
    private SpvBalanceFetcher spvBalanceFetcher;
    private int blockChainHeight;
    private boolean visible;

    @Override
    public String getAccountDefaultCurrency() {
        return CurrencyValue.BCH;
    }

    public Bip44BCHAccount(Bip44AccountContext context, Bip44AccountKeyManager keyManager,
                           NetworkParameters network, Bip44AccountBacking backing, Wapi wapi,
                           SpvBalanceFetcher spvBalanceFetcher) {
        super(context, keyManager, network, backing, wapi);
        this.spvBalanceFetcher = spvBalanceFetcher;
        this.type = Type.BCHBIP44;
    }

    @Override
    public CurrencyBasedBalance getCurrencyBasedBalance() {
        if (getAccountType() == ACCOUNT_TYPE_FROM_MASTERSEED) {
            return spvBalanceFetcher.retrieveByHdAccountIndex(getId().toString(), getAccountIndex());
        } else {
            return spvBalanceFetcher.retrieveByUnrelatedAccountId(getId().toString());
        }
    }

    @Override
    public TransactionDetails getTransactionDetails(Sha256Hash txid) {
        return spvBalanceFetcher.retrieveTransactionDetails(txid);
    }

    @Override
    public TransactionSummary getTransactionSummary(Sha256Hash txid) {
        List<TransactionSummary> transactions = spvBalanceFetcher.retrieveTransactionsSummaryByHdAccountIndex(getId().toString(),
                                                getAccountIndex());
        for (TransactionSummary transaction : transactions) {
            if(transaction.txid.equals(txid)) {
                return transaction;
            }
        }
        return null;
    }

    @Override
    public ExactCurrencyValue calculateMaxSpendableAmount(long minerFeePerKbToUse) {
        //TODO Refactor the code and make the proper usage of minerFeePerKbToUse parameter
        String txFee = "NORMAL";
        float txFeeFactor = 1.0f;
        if (getAccountType() == ACCOUNT_TYPE_FROM_MASTERSEED) {
            return ExactBitcoinCashValue.from(spvBalanceFetcher.calculateMaxSpendableAmount(getAccountIndex(), txFee, txFeeFactor));
        } else {
            return ExactBitcoinCashValue.from(spvBalanceFetcher.calculateMaxSpendableAmountUnrelatedAccount(getId().toString(), txFee, txFeeFactor));
        }
    }

    @Override
    public UUID getId() {
        return UUID.nameUUIDFromBytes(("BCH" + super.getId().toString()).getBytes());
    }

    // need override because parent write it to context(bch and btc account have one context)
    @Override
    public void setBlockChainHeight(int blockHeight) {
        blockChainHeight = blockHeight;
    }

    @Override
    public int getBlockChainHeight() {
        return blockChainHeight;
    }

    @Override
    public List<TransactionSummary> getTransactionHistory(int offset, int limit) {
        if (getAccountType() == ACCOUNT_TYPE_FROM_MASTERSEED) {
            return filterBtcTransactions(spvBalanceFetcher.retrieveTransactionsSummaryByHdAccountIndex(getId().toString(), getAccountIndex(), offset, limit));
        } else {
            return filterBtcTransactions(spvBalanceFetcher.retrieveTransactionsSummaryByUnrelatedAccountId(getId().toString(), offset, limit));
        }
    }

    private List<TransactionSummary> filterBtcTransactions(List<TransactionSummary> transactionSummaries) {
        List<TransactionSummary> filteredTransactions = new ArrayList<>(transactionSummaries);
        for (TransactionSummary transactionSummary : transactionSummaries) {
            final int forkBlock = 478559;
            if (transactionSummary.height < forkBlock) {
                filteredTransactions.remove(transactionSummary);
            }
        }
        return filteredTransactions;
    }

    @Override
    public List<TransactionSummary> getTransactionsSince(Long receivingSince) {
        if (getAccountType() == ACCOUNT_TYPE_FROM_MASTERSEED) {
            return spvBalanceFetcher.retrieveTransactionsSummaryByHdAccountIndex(getId().toString(), getAccountIndex(), receivingSince);
        } else {
            return spvBalanceFetcher.retrieveTransactionsSummaryByUnrelatedAccountId(getId().toString(), receivingSince);
        }
    }

    @Override
    public boolean isVisible() {
        if (!visible && (spvBalanceFetcher.getSyncProgressPercents() == 100 || spvBalanceFetcher.isAccountSynced(this))) {
            if (getAccountType() == ACCOUNT_TYPE_FROM_MASTERSEED) {
                visible = !spvBalanceFetcher.retrieveTransactionsSummaryByHdAccountIndex(getId().toString(), getAccountIndex()).isEmpty();
            } else {
                visible = !spvBalanceFetcher.retrieveTransactionsSummaryByUnrelatedAccountId(getId().toString()).isEmpty();
            }
        }
        return visible;
    }

    @Override
    public int getPrivateKeyCount() {
        if (getAccountType() == ACCOUNT_TYPE_FROM_MASTERSEED) {
            IssuedKeysInfo info = spvBalanceFetcher.getPrivateKeysCount(getAccountIndex());
            return info.getExternalKeys() + info.getInternalKeys();
        } else {
            IssuedKeysInfo info = spvBalanceFetcher.getPrivateKeysCountUnrelated(getId().toString());
            return info.getExternalKeys() + info.getInternalKeys();
        }
    }

    @Override
    public Optional<Address> getReceivingAddress() {
        return Optional.fromNullable(spvBalanceFetcher.getCurrentReceiveAddress(getAccountIndex()));
    }

    @Override
    public InMemoryPrivateKey getPrivateKeyForAddress(Address address, KeyCipher cipher) throws KeyCipher.InvalidKeyCipher {
        IssuedKeysInfo info = spvBalanceFetcher.getPrivateKeysCount(getAccountIndex());
        List<Address> internalAddresses = getAddressRange(true, 0, info.getInternalKeys() + INTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH);
        List<Address> externalAddresses = getAddressRange(false, 0, info.getExternalKeys() + EXTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH);

        int iix = internalAddresses.indexOf(address);

        if (iix != -1) {
            return _keyManager.getPrivateKey(true, iix, cipher);
        }

        int eix = externalAddresses.indexOf(address);

        if (eix != -1) {
            return _keyManager.getPrivateKey(false, eix, cipher);
        }

        return null;
    }
}
