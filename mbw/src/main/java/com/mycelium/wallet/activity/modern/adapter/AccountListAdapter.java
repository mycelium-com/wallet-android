package com.mycelium.wallet.activity.modern.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mycelium.wallet.AccountManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.RecordRowBuilder;
import com.mycelium.wallet.activity.modern.adapter.holder.AccountViewHolder;
import com.mycelium.wallet.activity.modern.adapter.holder.ArchivedGroupTitleViewHolder;
import com.mycelium.wallet.activity.modern.adapter.holder.GroupTitleViewHolder;
import com.mycelium.wallet.activity.modern.adapter.holder.TotalViewHolder;
import com.mycelium.wallet.activity.modern.model.ViewAccountModel;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.currency.CurrencySum;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AccountListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int GROUP_TITLE_TYPE = 2;
    private static final int ACCOUNT_TYPE = 3;
    private static final int TOTAL_BALANCE_TYPE = 4;
    private static final int GROUP_ARCHIVED_TITLE_TYPE = 5;

    private List<Item> itemList = new ArrayList<>();
    private UUID focusedAccountId;

    private ItemClickListener itemClickListener;
    private ItemSelectListener itemSelectListener;

    static class Item {
        int type;
        ViewAccountModel walletAccount;

        String title;
        List<ViewAccountModel> walletAccountList;

        public Item(int type, ViewAccountModel walletAccount) {
            this.type = type;
            this.walletAccount = walletAccount;
        }

        public Item(int type, String title, List<ViewAccountModel> walletAccountList) {
            this.type = type;
            this.title = title;
            this.walletAccountList = walletAccountList;
        }
    }

    private MbwManager mbwManager;
    private RecordRowBuilder builder;
    private LayoutInflater layoutInflater;
    private Context context;
    private SharedPreferences pagePrefs;

    public AccountListAdapter(Context context, MbwManager mbwManager) {
        this.mbwManager = mbwManager;
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        builder = new RecordRowBuilder(mbwManager, context.getResources());
        pagePrefs = context.getSharedPreferences("account_list", Context.MODE_PRIVATE);
    }

    public void setItemClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public void setItemSelectListener(ItemSelectListener itemSelectListener) {
        this.itemSelectListener = itemSelectListener;
    }

    public WalletAccount getFocusedAccount() {
        return mbwManager.getWalletManager(false).getAccount(focusedAccountId);
    }

    public void setFocusedAccountId(UUID focusedAccountId) {
        int oldFocusedPosition = findPosition(this.focusedAccountId);
        this.focusedAccountId = focusedAccountId;
        notifyItemChanged(oldFocusedPosition);
        notifyItemChanged(findPosition(this.focusedAccountId));
    }

    private int findPosition(UUID account) {
        int position = -1;
        for (int i = 0; i < itemList.size(); i++) {
            Item item = itemList.get(i);
            if (item.walletAccount != null
                    && Objects.equals(item.walletAccount.accountId, account)) {
                position = i;
                break;
            }
        }
        return position;
    }

    private static class DataUpdateAsyncTask extends AsyncTask<Void, List<Item>, List<Item>> {
        WeakReference<Context> wrContext;
        WeakReference<AccountListAdapter> wrAdapter;

        DataUpdateAsyncTask(Context context, AccountListAdapter adapter) {
            wrContext = new WeakReference<>(context);
            wrAdapter = new WeakReference<>(adapter);
        }

        private void checkWeakReferences() throws WeakReferenceNullException {
            if(wrContext.get() == null || wrAdapter.get() == null) {
                throw new WeakReferenceNullException();
            }
        }

        @Override
        protected List<Item> doInBackground(Void... voids) {
            try {
                AccountManager am = AccountManager.INSTANCE;

                List<Item> result = new ArrayList<>(addGroup(R.string.active_hd_accounts_name, GROUP_TITLE_TYPE, am.getBTCBip44Accounts().values()));
                checkWeakReferences();
                result.addAll(addGroup(wrContext.get().getString(R.string.active_bitcoin_sa_group_name), GROUP_TITLE_TYPE, am.getBTCSingleAddressAccounts().values()));
                checkWeakReferences();
                if(wrAdapter.get().itemList.isEmpty()) {
                    publishProgress(result);
                }
                result.addAll(addGroup(R.string.bitcoin_cash_hd, GROUP_TITLE_TYPE, am.getBCHBip44Accounts().values()));
                result.addAll(addGroup(R.string.bitcoin_cash_sa, GROUP_TITLE_TYPE, am.getBCHSingleAddressAccounts().values()));

                List<WalletAccount> coluAccounts = new ArrayList<>();
                for (WalletAccount walletAccount : am.getColuAccounts().values()) {
                    coluAccounts.add(walletAccount);
                    coluAccounts.add(((ColuAccount) walletAccount).getLinkedAccount());
                }
                result.addAll(addGroup(R.string.digital_assets, GROUP_TITLE_TYPE, coluAccounts));

                List<WalletAccount> accounts = am.getActiveAccounts().values().asList();
                List<WalletAccount> other = new ArrayList<>();
                for (WalletAccount account : accounts) {
                    switch (account.getType()) {
                        case BTCSINGLEADDRESS:
                        case BTCBIP44:
                        case BCHSINGLEADDRESS:
                        case BCHBIP44:
                        case COLU:
                            break;
                        default:
                            other.add(account);
                            break;
                    }
                }
                result.addAll(addGroup(R.string.active_other_accounts_name, GROUP_TITLE_TYPE, other));

                checkWeakReferences();
                result.add(new Item(TOTAL_BALANCE_TYPE, "", wrAdapter.get().builder.convertList(am.getActiveAccounts().values().asList())));
                result.addAll(addGroup(R.string.archive_name, GROUP_ARCHIVED_TITLE_TYPE, am.getArchivedAccounts().values()));
                return result;
            } catch (WeakReferenceNullException ignore) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Item> items) {
            super.onPostExecute(items);
            try {
                checkWeakReferences();
            } catch (WeakReferenceNullException ignore) {
                return;
            }
            wrAdapter.get().itemList = items;
            wrAdapter.get().notifyDataSetChanged();
        }

        @SafeVarargs
        @Override
        protected final void onProgressUpdate(List<Item>... values) {
            super.onProgressUpdate(values);
            if(wrAdapter.get() == null || wrContext.get() == null) {
                return;
            }
            wrAdapter.get().itemList = values[0];
            wrAdapter.get().notifyDataSetChanged();
        }

        private List<Item> addGroup(int titleId, int titleType, Collection<WalletAccount> accounts) throws WeakReferenceNullException {
            checkWeakReferences();
            return addGroup(wrContext.get().getString(titleId), titleType, accounts);
        }

        private List<Item> addGroup(String title, int titleType, Collection<WalletAccount> accounts) throws WeakReferenceNullException {
            checkWeakReferences();
            MetadataStorage storage = wrAdapter.get().mbwManager.getMetadataStorage();
            return buildGroup(new ArrayList<>(accounts), storage, title, titleType);
        }

        private List<Item> buildGroup(List<WalletAccount> accountList, MetadataStorage storage, String title, int type) throws WeakReferenceNullException {
            List<WalletAccount> accounts = Utils.sortAccounts(accountList, storage);

            checkWeakReferences();
            List<ViewAccountModel> viewAccountList = wrAdapter.get().builder.convertList(accounts);

            List<Item> result = new ArrayList<>();
            if (!viewAccountList.isEmpty()) {
                result.add(new Item(type, title, viewAccountList));
                if (wrAdapter.get().pagePrefs.getBoolean(title, true)) {
                    for (ViewAccountModel account : viewAccountList) {
                        result.add(new Item(ACCOUNT_TYPE, account));
                    }
                }
            }
            return result;
        }

        private class WeakReferenceNullException extends Exception {};
    }

    public void updateData() {
        new DataUpdateAsyncTask(context, this).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder result = null;
        if (viewType == GROUP_TITLE_TYPE) {
            View view = layoutInflater.inflate(R.layout.accounts_title_view, parent, false);
            GroupTitleViewHolder res = new GroupTitleViewHolder(view);
            res.tvBalance.setEventBus(mbwManager.getEventBus());
            res.tvBalance.setCurrencySwitcher(mbwManager.getCurrencySwitcher());
            result = res;
        } else if (viewType == GROUP_ARCHIVED_TITLE_TYPE) {
            View view = layoutInflater.inflate(R.layout.accounts_archived_title_view, parent, false);
            result = new ArchivedGroupTitleViewHolder(view);
        } else if (viewType == ACCOUNT_TYPE) {
            View view = layoutInflater.inflate(R.layout.record_row, parent, false);
            result = new AccountViewHolder(view);
        } else if (viewType == TOTAL_BALANCE_TYPE) {
            View view = layoutInflater.inflate(R.layout.record_row_total, parent, false);
            TotalViewHolder res = new TotalViewHolder(view);
            res.tcdBalance.setCurrencySwitcher(mbwManager.getCurrencySwitcher());
            res.tcdBalance.setEventBus(mbwManager.getEventBus());
            result = res;
        }
        return result;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final Item item = itemList.get(position);
        int viewType = item.type;
        if (viewType == ACCOUNT_TYPE) {
            AccountViewHolder accountHolder = (AccountViewHolder) holder;
            final ViewAccountModel account = item.walletAccount;
            builder.buildRecordView(accountHolder, account
                    , Objects.equals(mbwManager.getSelectedAccount().getId(), account.accountId)
                    , Objects.equals(focusedAccountId, account.accountId));
            accountHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setFocusedAccountId(account.accountId);
                    if (itemSelectListener != null) {
                        itemSelectListener.onClick(mbwManager.getWalletManager(false)
                                .getAccount(account.accountId));
                    }

                }
            });
            accountHolder.llAddress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setFocusedAccountId(account.accountId);
                    if (itemClickListener != null) {
                        itemClickListener.onItemClick(mbwManager.getWalletManager(false)
                                .getAccount(account.accountId));
                    }
                }
            });
        } else if (viewType == GROUP_TITLE_TYPE) {
            GroupTitleViewHolder groupHolder = (GroupTitleViewHolder) holder;
            groupHolder.tvTitle.setText(Html.fromHtml(item.title));
            int count = item.walletAccountList.size();
            groupHolder.tvAccountsCount.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
            groupHolder.tvAccountsCount.setText("(" + count + ")");
            groupHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean isGroupVisible = !pagePrefs.getBoolean(item.title, true);
                    pagePrefs.edit().putBoolean(item.title, isGroupVisible).apply();
                    updateData();
                }
            });
            groupHolder.expandIcon.setRotation(pagePrefs.getBoolean(item.title, true) ? 180 : 0);
            CurrencySum sum = getSpendableBalance(item.walletAccountList);
            if (sum != null) {
                groupHolder.tvBalance.setValue(sum);
                groupHolder.tvBalance.setVisibility(View.VISIBLE);
            } else {
                groupHolder.tvBalance.setVisibility(View.GONE);
            }
        } else if (viewType == GROUP_ARCHIVED_TITLE_TYPE) {
            ArchivedGroupTitleViewHolder groupHolder = (ArchivedGroupTitleViewHolder) holder;
            groupHolder.tvTitle.setText(Html.fromHtml(item.title));
            int count = item.walletAccountList.size();
            groupHolder.tvAccountsCount.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
            groupHolder.tvAccountsCount.setText("(" + count + ")");
            groupHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean isGroupVisible = !pagePrefs.getBoolean(item.title, true);
                    pagePrefs.edit().putBoolean(item.title, isGroupVisible).apply();
                    updateData();
                }
            });
            groupHolder.expandIcon.setRotation(pagePrefs.getBoolean(item.title, true) ? 180 : 0);
        } else if (viewType == TOTAL_BALANCE_TYPE) {
            TotalViewHolder totalHolder = (TotalViewHolder) holder;
            CurrencySum sum = getSpendableBalance(item.walletAccountList);
            if (sum != null) {
                totalHolder.tcdBalance.setValue(sum);
            }
        }
    }

    private CurrencySum getSpendableBalance(List<ViewAccountModel> walletAccountList) {
        CurrencySum currencySum = new CurrencySum();
        for (ViewAccountModel account : walletAccountList) {
            if (account.isActive) {
                currencySum.add(account.balance.confirmed);
            }
        }
        return currencySum;
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return itemList.get(position).type;
    }

    public interface ItemClickListener {
        void onItemClick(WalletAccount account);
    }

    public interface ItemSelectListener {
        void onClick(WalletAccount account);
    }
}
