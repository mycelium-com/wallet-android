package com.mycelium.wallet.activity.modern.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.RecordRowBuilder;
import com.mycelium.wallet.activity.util.ToggleableCurrencyButton;
import com.mycelium.wallet.activity.util.ToggleableCurrencyDisplay;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.currency.CurrencySum;

import java.util.ArrayList;
import java.util.List;

public class AccountListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int GROUP_TITLE_TYPE = 2;
    private static final int ACCOUNT_TYPE = 3;
    private static final int TOTAL_BALANCE_TYPE = 4;
    private static final int GROUP_ARCHIVED_TITLE_TYPE = 5;

    private List<Item> itemList = new ArrayList<>();
    private WalletAccount focusedAccount;

    private ItemClickListener itemClickListener;

    class Item {
        int type;
        WalletAccount walletAccount;

        String title;
        List<WalletAccount> walletAccountList;

        public Item(int type, WalletAccount walletAccount) {
            this.type = type;
            this.walletAccount = walletAccount;
        }

        public Item(int type, String title, List<WalletAccount> walletAccountList) {
            this.type = type;
            this.title = title;
            this.walletAccountList = walletAccountList;
        }
    }

    private MbwManager mbwManager;
    private RecordRowBuilder builder;
    private LayoutInflater layoutInflater;
    private Context context;

    public AccountListAdapter(Context context, MbwManager mbwManager) {
        this.mbwManager = mbwManager;
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        builder = new RecordRowBuilder(mbwManager, context.getResources(), layoutInflater);
    }

    public void setItemClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public WalletAccount getFocusedAccount() {
        return focusedAccount;
    }

    public void setFocusedAccount(WalletAccount focusedAccount) {
        notifyItemChanged(findPosition(this.focusedAccount));
        this.focusedAccount = focusedAccount;
        notifyItemChanged(findPosition(this.focusedAccount));
    }

    private int findPosition(WalletAccount account) {
        int position = -1;
        for (int i = 0; i < itemList.size(); i++) {
            Item item = itemList.get(i);
            if (item.walletAccount == account) {
                position = i;
                break;
            }
        }
        return position;
    }

    public void updateData() {
        itemList.clear();
        WalletManager walletManager = mbwManager.getWalletManager(false);
        MetadataStorage storage = mbwManager.getMetadataStorage();

        List<WalletAccount> activeHdRecords = Utils.sortAccounts(walletManager.getActiveMasterseedAccounts(), storage);

        if (!activeHdRecords.isEmpty()) {
            itemList.add(new Item(GROUP_TITLE_TYPE, context.getString(R.string.active_hd_accounts_name), activeHdRecords));
            for (WalletAccount account : activeHdRecords) {
                itemList.add(new Item(ACCOUNT_TYPE, account));
            }
        }

        List<WalletAccount> activeOtherRecords = Utils.sortAccounts(walletManager.getActiveOtherAccounts(), storage);
        if (!activeOtherRecords.isEmpty()) {
            itemList.add(new Item(GROUP_TITLE_TYPE, context.getString(R.string.active_other_accounts_name), activeOtherRecords));
            for (WalletAccount account : activeOtherRecords) {
                itemList.add(new Item(ACCOUNT_TYPE, account));
            }
        }

        List<WalletAccount> allAccount = new ArrayList<>();
        allAccount.addAll(activeHdRecords);
        allAccount.addAll(activeOtherRecords);
        itemList.add(new Item(TOTAL_BALANCE_TYPE, "", allAccount));

        List<WalletAccount> archivedRecords = Utils.sortAccounts(walletManager.getArchivedAccounts(), storage);
        if (!archivedRecords.isEmpty()) {
            itemList.add(new Item(GROUP_ARCHIVED_TITLE_TYPE, "", archivedRecords));
            for (WalletAccount account : archivedRecords) {
                itemList.add(new Item(ACCOUNT_TYPE, account));
            }
        }

        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder result = null;
        if (viewType == GROUP_TITLE_TYPE || viewType == GROUP_ARCHIVED_TITLE_TYPE) {
            View view = layoutInflater.inflate(R.layout.accounts_title_view, parent, false);
            GroupTitleViewHolder res = new GroupTitleViewHolder(view);
            res.tvBalance.setEventBus(mbwManager.getEventBus());
            res.tvBalance.setCurrencySwitcher(mbwManager.getCurrencySwitcher());
            result = res;
        } else if (viewType == ACCOUNT_TYPE) {
            View view = layoutInflater.inflate(R.layout.record_row, parent, false);
            result = new AccountViewHolder(view);
        } else if (viewType == TOTAL_BALANCE_TYPE) {
            View view = layoutInflater.inflate(R.layout.record_row_total, parent, false);
            TotalViewHolder res = new TotalViewHolder(view);
            res.tcdBalance.setCurrencySwitcher(mbwManager.getCurrencySwitcher());
            result = res;
        }
        return result;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Item item = itemList.get(position);
        int viewType = item.type;
        if (viewType == ACCOUNT_TYPE) {
            final WalletAccount account = item.walletAccount;
            builder.buildRecordView(null, account, mbwManager.getSelectedAccount() == account
                    , focusedAccount == account, holder.itemView);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setFocusedAccount(account);
                    if (itemClickListener != null) {
                        itemClickListener.onItemClick(account);
                    }
                }
            });
        } else if (viewType == GROUP_TITLE_TYPE) {
            GroupTitleViewHolder groupHolder = (GroupTitleViewHolder) holder;
            groupHolder.tvTitle.setText(item.title);

            CurrencySum sum = getSpendableBalance(item.walletAccountList);
            if (sum != null) {
                groupHolder.tvBalance.setValue(sum);
                groupHolder.tvBalance.setVisibility(View.VISIBLE);
            } else {
                groupHolder.tvBalance.setVisibility(View.GONE);
            }
        } else if (viewType == GROUP_ARCHIVED_TITLE_TYPE) {
            GroupTitleViewHolder groupHolder = (GroupTitleViewHolder) holder;
            groupHolder.tvTitle.setText(item.title);
            groupHolder.tvBalance.setVisibility(View.GONE);
        } else if (viewType == TOTAL_BALANCE_TYPE) {
            TotalViewHolder totalHolder = (TotalViewHolder) holder;
            CurrencySum sum = getSpendableBalance(item.walletAccountList);
            if (sum != null) {
                totalHolder.tcdBalance.setValue(sum);
            }
        }
    }

    private CurrencySum getSpendableBalance(List<WalletAccount> walletAccountList) {
        CurrencySum currencySum = new CurrencySum();
        for (WalletAccount account : walletAccountList) {
            currencySum.add(account.getCurrencyBasedBalance().confirmed);
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

    private static class GroupTitleViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        ToggleableCurrencyDisplay tvBalance;

        public GroupTitleViewHolder(View itemView) {
            super(itemView);
            tvTitle = (TextView) itemView.findViewById(R.id.tvTitle);
            tvBalance = (ToggleableCurrencyDisplay) itemView.findViewById(R.id.tvBalance);
        }
    }

    private static class TotalViewHolder extends RecyclerView.ViewHolder {
        ToggleableCurrencyButton tcdBalance;

        public TotalViewHolder(View itemView) {
            super(itemView);
            tcdBalance = (ToggleableCurrencyButton) itemView.findViewById(R.id.tcdBalance);
        }
    }

    public static class AccountViewHolder extends RecyclerView.ViewHolder {

        public AccountViewHolder(View itemView) {
            super(itemView);
        }
    }

    public interface ItemClickListener {
        void onItemClick(WalletAccount account);
    }
}
