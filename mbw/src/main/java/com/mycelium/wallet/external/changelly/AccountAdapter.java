package com.mycelium.wallet.external.changelly;


import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.util.CoinUtil;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView;
import com.mycelium.wapi.wallet.AbstractAccount;
import com.mycelium.wapi.wallet.WalletAccount;

import java.util.ArrayList;
import java.util.List;

public class AccountAdapter extends SelectableRecyclerView.Adapter<RecyclerView.ViewHolder> {
    public enum AccountUseType {
        OUT(R.drawable.sender_recyclerview_item_background_selector_red
                , R.drawable.recyclerview_item_bottom_rectangle_selector
                , Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, R.dimen.recycler_item_rectangle_height
                , R.layout.list_item_padding_sending),
        IN(R.drawable.sender_recyclerview_item_background_selector2
                , R.drawable.recyclerview_item_top_rectangle_selector
                , Gravity.TOP | Gravity.CENTER_HORIZONTAL, R.dimen.recycler_item_triangle_height
                , R.layout.list_item_padding_receiving);
        public int background;
        public int gravity;
        public int heightRes;
        public int indicatorImg;
        public int paddingLayout;

        AccountUseType(int background, int indicatorImg, int gravity, int height, int paddingLayout) {
            this.background = background;
            this.indicatorImg = indicatorImg;
            this.gravity = gravity;
            this.heightRes = height;
            this.paddingLayout = paddingLayout;
        }
    }

    private List<Item> items = new ArrayList<>();
    private int paddingWidth = 0;
    private MbwManager mbwManager;
    private AccountUseType accountUseType = AccountUseType.IN;

    public void setAccountUseType(AccountUseType accountUseType) {
        this.accountUseType = accountUseType;
    }

    public AccountAdapter(MbwManager mbwManager, List<WalletAccount> accounts, int paddingWidth) {
        this.mbwManager = mbwManager;
        this.paddingWidth = paddingWidth;
        items.add(new Item(null, VIEW_TYPE_PADDING));
        accounts = Utils.sortAccounts(accounts, mbwManager.getMetadataStorage());
        for (WalletAccount account : accounts) {
            items.add(new Item(account, VIEW_TYPE_ITEM));
        }
        items.add(new Item(null, VIEW_TYPE_PADDING));
    }

    public Item getItem(int selectedItem) {
        return items.get(selectedItem);
    }

    public static class Item {
        public Item(WalletAccount account, int type) {
            this.type = type;
            this.account = account;
        }

        public int type;
        public WalletAccount account;
    }

    @Override
    public int findIndex(Object selected) {
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            if (item.account != null && item.account == selected) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recyclerview_item_fee_lvl, parent, false);
            v.setBackgroundResource(accountUseType.background);
            ImageView imageView = v.findViewById(R.id.rectangle);
            imageView.setImageResource(accountUseType.indicatorImg);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) imageView.getLayoutParams();
            layoutParams.gravity = accountUseType.gravity;
            layoutParams.height = parent.getResources().getDimensionPixelSize(accountUseType.heightRes);
            imageView.setLayoutParams(layoutParams);
            return new ViewHolder(v);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(accountUseType.paddingLayout,
                    parent, false);
            view.setBackgroundResource(accountUseType.background);
            return new PaddingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            ViewHolder viewHolder = (ViewHolder) holder;

            Item item = items.get(position);
            viewHolder.categoryTextView.setText(mbwManager.getMetadataStorage().getLabelByAccount(item.account.getId()));
            CoinUtil.Denomination denomination = mbwManager.getBitcoinDenomination();
            viewHolder.itemTextView.setText(Utils.getFormattedValueWithUnit(item.account.getCurrencyBasedBalance().confirmed, denomination));
            if (item.account instanceof AbstractAccount) {
                AbstractAccount account = (AbstractAccount) item.account;
                if (!trySettingReceivingAddress(viewHolder, account.getReceivingAddress(AddressType.P2SH_P2WPKH))) {
                    trySettingReceivingAddress(viewHolder, account.getReceivingAddress(AddressType.P2PKH));
                }
            } else {
                if (item.account.getReceivingAddress().isPresent()) {
                    viewHolder.valueTextView.setText(item.account.getReceivingAddress().get().toString());
                }
            }
        } else {
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.width = paddingWidth;
            holder.itemView.setLayoutParams(layoutParams);
            holder.itemView.setVisibility(getItemCount() > 3 || position == 0 ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private boolean trySettingReceivingAddress(ViewHolder viewHolder, Address receivingAddress) {
        if (receivingAddress != null) {
            viewHolder.valueTextView.setText(receivingAddress.toString());
            return true;
        }
        return false;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView categoryTextView;
        public TextView itemTextView;
        public TextView valueTextView;

        public ViewHolder(View v) {
            super(v);
            categoryTextView = v.findViewById(R.id.categorytextView);
            itemTextView = v.findViewById(R.id.itemTextView);
            valueTextView = (TextView) v.findViewById(R.id.valueTextView);
        }
    }

    public static class PaddingViewHolder extends RecyclerView.ViewHolder {
        public PaddingViewHolder(View v) {
            super(v);
        }
    }
}
