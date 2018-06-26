package com.mycelium.wapi.wallet.single;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.model.TransactionDetails;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.SingleAddressAccountBacking;
import com.mycelium.wapi.wallet.SpvBalanceFetcher;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinCashValue;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SingleAddressBCHAccount extends SingleAddressAccount {
    private SpvBalanceFetcher spvBalanceFetcher;
    private boolean visible;

    public SingleAddressBCHAccount(SingleAddressAccountContext context,
                                   PublicPrivateKeyStore keyStore, NetworkParameters network,
                                   SingleAddressAccountBacking backing, Wapi wapi,
                                   SpvBalanceFetcher spvBalanceFetcher) {
        super(context, keyStore, network, backing, wapi);
        this.spvBalanceFetcher = spvBalanceFetcher;
        this.type = Type.BCHSINGLEADDRESS;
    }

    @Override
    public String getAccountDefaultCurrency() {
        return CurrencyValue.BCH;
    }

    @Override
    public CurrencyBasedBalance getCurrencyBasedBalance() {
        return spvBalanceFetcher.retrieveByUnrelatedAccountId(getId().toString());
    }

    @Override
    public ExactCurrencyValue calculateMaxSpendableAmount(long minerFeePerKbToUse) {
        //TODO Refactor the code and make the proper usage of minerFeePerKbToUse parameter
        String txFee = "NORMAL";
        float txFeeFactor = 1.0f;
        return ExactBitcoinCashValue.from(spvBalanceFetcher.calculateMaxSpendableAmountUnrelatedAccount(getId().toString(), txFee, txFeeFactor));
    }

    @Override
    public UUID getId() {
        return UUID.nameUUIDFromBytes(("BCH" + super.getId().toString()).getBytes());
    }

    public static UUID calculateId(Address address) {
        return UUID.nameUUIDFromBytes(("BCH" + SingleAddressAccount.calculateId(address).toString()).getBytes());
    }

    @Override
    public List<TransactionSummary> getTransactionHistory(int offset, int limit) {
        return filterBtcTransactions(spvBalanceFetcher.retrieveTransactionsSummaryByUnrelatedAccountId(getId().toString(), offset, limit));
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
        return spvBalanceFetcher.retrieveTransactionsSummaryByUnrelatedAccountId(getId().toString(), receivingSince);
    }

    @Override
    public TransactionDetails getTransactionDetails(Sha256Hash txid) {
        return spvBalanceFetcher.retrieveTransactionDetails(txid);
    }

    @Override
    public TransactionSummary getTransactionSummary(Sha256Hash txid) {
        List<TransactionSummary> transactions = spvBalanceFetcher.retrieveTransactionsSummaryByUnrelatedAccountId(getId().toString());
        for (TransactionSummary transaction : transactions) {
            if(transaction.txid.equals(txid)) {
                return transaction;
            }
        }
        return null;
    }

    @Override
    public boolean isVisible() {
        if (!visible && (spvBalanceFetcher.getSyncProgressPercents() == 100 || spvBalanceFetcher.isAccountSynced(this))) {
            visible = !spvBalanceFetcher.retrieveTransactionsSummaryByUnrelatedAccountId(getId().toString()).isEmpty();
        }
        return visible;
    }
}
