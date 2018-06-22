package com.mycelium.wallet.activity.main;

import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.WalletAccount;

import java.util.Collections;
import java.util.List;

class UpdateTxHistoryTask extends AsyncTask<Void, Void, List<TransactionSummary>> {
    private final TransactionHistoryFragment fragment;
    private final MbwManager mbwManager;
    private List<TransactionSummary> history;
    private WalletAccount account;
    private TransactionHistoryFragment.Wrapper wrapper;

    UpdateTxHistoryTask(TransactionHistoryFragment fragment, MbwManager mbwManager, TransactionHistoryFragment.Wrapper wrapper,
                        List<TransactionSummary> history) {
        super();
        this.fragment = fragment;
        this.mbwManager = mbwManager;
        this.wrapper = wrapper;
        this.history = history;
    }

    @Override
    protected void onPreExecute() {
        if (!fragment.isAdded()) {
            cancel(true);
        }
        account = mbwManager.getSelectedAccount();
        if (account.isArchived()) {
            fragment.showHistory(false);
            cancel(true);
        }
        if (wrapper == null) {
            fragment.showHistory(true);
            wrapper = fragment.new Wrapper(fragment.getActivity(), history);
            fragment.updateWrapper(wrapper);
        }
    }

    @Override
    protected List<TransactionSummary> doInBackground(Void... voids) {
        return account.getTransactionHistory(0, Math.max(20, history.size()));
    }

    @Override
    protected void onPostExecute(List<TransactionSummary> transactionSummaries) {
        history.clear();
        history.addAll(transactionSummaries);
        Collections.sort(history);
        Collections.reverse(history);
        if (history.isEmpty()) {
            fragment.showHistory(false);
        } else {
            fragment.showHistory(true);
            //wrapper.notifyDataSetChanged();
        }
        fragment.refreshList();
        FragmentActivity activity = fragment.getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
    }
}

