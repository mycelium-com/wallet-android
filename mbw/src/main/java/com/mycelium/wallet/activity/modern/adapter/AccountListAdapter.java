package com.mycelium.wallet.activity.modern.adapter;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.modern.RecordRowBuilder;
import com.mycelium.wallet.activity.modern.adapter.holder.AccountViewHolder;
import com.mycelium.wallet.activity.modern.adapter.holder.ArchivedGroupTitleViewHolder;
import com.mycelium.wallet.activity.modern.adapter.holder.GroupTitleViewHolder;
import com.mycelium.wallet.activity.modern.adapter.holder.TotalViewHolder;
import com.mycelium.wallet.activity.modern.model.accounts.AccountItem;
import com.mycelium.wallet.activity.modern.model.accounts.AccountsListModel;
import com.mycelium.wallet.activity.modern.model.ViewAccountModel;
import com.mycelium.wallet.event.AccountGroupCollapsed;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.currency.CurrencySum;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AccountListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int GROUP_TITLE_TYPE = 2;
    public static final int ACCOUNT_TYPE = 3;
    public static final int TOTAL_BALANCE_TYPE = 4;
    public static final int GROUP_ARCHIVED_TITLE_TYPE = 5;

    private List<AccountItem> itemList = new ArrayList<>();
    private UUID focusedAccountId;

    private ItemClickListener itemClickListener;
    private ItemSelectListener itemSelectListener;

    private MbwManager mbwManager;
    private RecordRowBuilder builder;
    private LayoutInflater layoutInflater;
    private SharedPreferences pagePrefs;

    public AccountListAdapter(Fragment fragment, MbwManager mbwManager) {
        AccountsListModel listModel = ViewModelProviders.of(fragment).get(AccountsListModel.class);
        Context context = fragment.getContext();
        this.mbwManager = mbwManager;

        layoutInflater = LayoutInflater.from(context);
        builder = new RecordRowBuilder(mbwManager, context.getResources());
        pagePrefs = context.getSharedPreferences("account_list", Context.MODE_PRIVATE);
        listModel.getAccountsData().observe(fragment, new Observer<List<? extends AccountItem>>() {
            @Override
            public void onChanged(List<? extends AccountItem> accountItems) {
                itemList.clear();
                itemList.addAll(accountItems);
                notifyDataSetChanged();
            }
        });
        itemList.addAll(listModel.getAccountsData().getValue());
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
        this.focusedAccountId = focusedAccountId;
        notifyDataSetChanged(); // Somewhy in other cases android would not update alpha value
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
        final AccountItem item = itemList.get(position);
        int viewType = item.getType();
        if (viewType == ACCOUNT_TYPE) {
            AccountViewHolder accountHolder = (AccountViewHolder) holder;
            final ViewAccountModel account = item.getWalletAccount();
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
            groupHolder.tvTitle.setText(Html.fromHtml(item.getTitle()));
            int count = item.getWalletAccountList().size();
            groupHolder.tvAccountsCount.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
            groupHolder.tvAccountsCount.setText("(" + count + ")");
            groupHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean isGroupVisible = !pagePrefs.getBoolean(item.getTitle(), true);
                    pagePrefs.edit().putBoolean(item.getTitle(), isGroupVisible).apply();
                    mbwManager.getEventBus().post(new AccountGroupCollapsed());
                }
            });
            groupHolder.expandIcon.setRotation(pagePrefs.getBoolean(item.getTitle(), true) ? 180 : 0);
            CurrencySum sum = getSpendableBalance(item.getWalletAccountList());
            groupHolder.tvBalance.setValue(sum);
            groupHolder.tvBalance.setVisibility(View.VISIBLE);
        } else if (viewType == GROUP_ARCHIVED_TITLE_TYPE) {
            ArchivedGroupTitleViewHolder groupHolder = (ArchivedGroupTitleViewHolder) holder;
            groupHolder.tvTitle.setText(Html.fromHtml(item.getTitle()));
            int count = item.getWalletAccountList().size();
            groupHolder.tvAccountsCount.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
            groupHolder.tvAccountsCount.setText("(" + count + ")");
            groupHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean isGroupVisible = !pagePrefs.getBoolean(item.getTitle(), true);
                    pagePrefs.edit().putBoolean(item.getTitle(), isGroupVisible).apply();
                    mbwManager.getEventBus().post(new AccountGroupCollapsed());
                }
            });
            groupHolder.expandIcon.setRotation(pagePrefs.getBoolean(item.getTitle(), true) ? 180 : 0);
        } else if (viewType == TOTAL_BALANCE_TYPE) {
            TotalViewHolder totalHolder = (TotalViewHolder) holder;
            CurrencySum sum = getSpendableBalance(item.getWalletAccountList());
            totalHolder.tcdBalance.setValue(sum);
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
        return itemList.get(position).getType();
    }

    public interface ItemClickListener {
        void onItemClick(WalletAccount account);
    }

    public interface ItemSelectListener {
        void onClick(WalletAccount account);
    }
}
