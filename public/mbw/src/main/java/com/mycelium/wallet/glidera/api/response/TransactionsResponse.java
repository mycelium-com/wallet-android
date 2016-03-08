package com.mycelium.wallet.glidera.api.response;

import java.util.List;

public class TransactionsResponse extends GlideraResponse {
    private List<TransactionResponse> transactions;

    public List<TransactionResponse> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionResponse> transactions) {
        this.transactions = transactions;
    }
}
