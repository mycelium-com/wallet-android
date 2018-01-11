package com.mycelium.wallet.activity.modern.adapter;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.mycelium.wallet.activity.modern.adapter.holder.GroupTitleViewHolder;
import com.mycelium.wallet.activity.modern.adapter.holder.TotalViewHolder;
import com.mycelium.wallet.colu.ColuAccount;
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
    private ItemSelectListener itemSelectListener;

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
    private SharedPreferences pagePrefs;

    public AccountListAdapter(Context context, MbwManager mbwManager) {
        this.mbwManager = mbwManager;
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        builder = new RecordRowBuilder(mbwManager, context.getResources(), layoutInflater);
        pagePrefs = context.getSharedPreferences("account_list", Context.MODE_PRIVATE);
    }

    public void setItemClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public void setItemSelectListener(ItemSelectListener itemSelectListener) {
        this.itemSelectListener = itemSelectListener;
    }

    public WalletAccount getFocusedAccount() {
        return focusedAccount;
    }

    public void setFocusedAccount(WalletAccount focusedAccount) {
        int oldFocusedPosition = findPosition(this.focusedAccount);
        this.focusedAccount = focusedAccount;
        notifyItemChanged(oldFocusedPosition);
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

        List<WalletAccount> bitcoinHdAccounts = new ArrayList<>(AccountManager.INSTANCE.getBTCBip44Accounts().values());
        itemList.addAll(buildGroup(bitcoinHdAccounts, storage, context.getString(R.string.active_hd_accounts_name), GROUP_TITLE_TYPE));

        List<WalletAccount> bitcoinSAAccounts = new ArrayList<>(AccountManager.INSTANCE.getBTCSingleAddressAccounts().values());
        itemList.addAll(buildGroup(bitcoinSAAccounts, storage, "Bitcoin SA", GROUP_TITLE_TYPE));

        List<WalletAccount> bitcoinCashHDAccounts = new ArrayList<>(AccountManager.INSTANCE.getBCHBip44Accounts().values());
        itemList.addAll(buildGroup(bitcoinCashHDAccounts, storage, context.getString(R.string.bitcoin_cash_hd), GROUP_TITLE_TYPE));

        List<WalletAccount> bitcoinCashSAAccounts = new ArrayList<>(AccountManager.INSTANCE.getBCHSingleAddressAccounts().values());
        itemList.addAll(buildGroup(bitcoinCashSAAccounts, storage, context.getString(R.string.bitcoin_cash_sa), GROUP_TITLE_TYPE));

        List<WalletAccount> coluAccounts = new ArrayList<>();
        for (WalletAccount walletAccount : AccountManager.INSTANCE.getColuAccounts().values()) {
            coluAccounts.add(walletAccount);
            coluAccounts.add(((ColuAccount)walletAccount).getLinkedAccount());
        }
        itemList.addAll(buildGroup(coluAccounts, storage, context.getString(R.string.digital_assets), GROUP_TITLE_TYPE));

        List<WalletAccount> accounts = walletManager.getActiveOtherAccounts();
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

        itemList.addAll(buildGroup(other, storage, context.getString(R.string.active_other_accounts_name), GROUP_TITLE_TYPE));

        itemList.add(new Item(TOTAL_BALANCE_TYPE, "", walletManager.getActiveAccounts()));

        itemList.addAll(buildGroup(walletManager.getArchivedAccounts(), storage
                , context.getString(R.string.archive_name), GROUP_ARCHIVED_TITLE_TYPE));

        notifyDataSetChanged();
    }

    public List<Item> buildGroup(List<WalletAccount> accountList, MetadataStorage storage, String title, int type) {
        List<WalletAccount> accounts = Utils.sortAccounts(accountList, storage);
        List<Item> result = new ArrayList<>();
        if (!accounts.isEmpty()) {
            result.add(new Item(type, title, accounts));
            if (pagePrefs.getBoolean(title, true)) {
                for (WalletAccount account : accounts) {
                    result.add(new Item(ACCOUNT_TYPE, account));
                }
            }
        }
        return result;
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
            final WalletAccount account = item.walletAccount;
            builder.buildRecordView(null, account, mbwManager.getSelectedAccount() == account
                    , focusedAccount == account, holder.itemView);
            accountHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setFocusedAccount(account);
                    if (itemSelectListener != null) {
                        itemSelectListener.onClick(account);
                    }

                }
            });
            accountHolder.llAddress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setFocusedAccount(account);
                    if (itemClickListener != null) {
                        itemClickListener.onItemClick(account);
                    }
                }
            });
        } else if (viewType == GROUP_TITLE_TYPE || viewType == GROUP_ARCHIVED_TITLE_TYPE) {
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
            if (viewType == GROUP_ARCHIVED_TITLE_TYPE) {
                groupHolder.tvBalance.setVisibility(View.GONE);
            } else {
                CurrencySum sum = getSpendableBalance(item.walletAccountList);
                if (sum != null) {
                    groupHolder.tvBalance.setValue(sum);
                    groupHolder.tvBalance.setVisibility(View.VISIBLE);
                } else {
                    groupHolder.tvBalance.setVisibility(View.GONE);
                }
            }
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

    public interface ItemClickListener {
        void onItemClick(WalletAccount account);
    }

    public interface ItemSelectListener {
        void onClick(WalletAccount account);
    }
}
