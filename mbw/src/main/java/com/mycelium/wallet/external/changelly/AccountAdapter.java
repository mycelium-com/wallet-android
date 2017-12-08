package com.mycelium.wallet.external.changelly;


import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.mrd.bitlib.util.CoinUtil;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.bip44.Bip44Account;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;

import java.util.ArrayList;
import java.util.List;

public class AccountAdapter extends SelectableRecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Item> items = new ArrayList<>();
    private int paddingWidth = 0;
    private MbwManager mbwManager;

    public AccountAdapter(MbwManager mbwManager, List<WalletAccount> accounts, int paddingWidth) {
        this.mbwManager = mbwManager;
        this.paddingWidth = paddingWidth;
        items.add(new Item(null, VIEW_TYPE_PADDING));
        accounts = Utils.sortAccounts(accounts, mbwManager.getMetadataStorage());
        for (WalletAccount account : accounts) {
            if (account instanceof Bip44Account || account instanceof SingleAddressAccount) {
                items.add(new Item(account, VIEW_TYPE_ITEM));
            }
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

        int type;
        WalletAccount account;
    }

    @Override
    public int findIndex(Object selected) {
        return 0;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recyclerview_item_fee_lvl, parent, false);
            v.setBackgroundResource(R.drawable.sender_recyclerview_item_background_selector2);
            ImageView imageView = (ImageView) v.findViewById(R.id.rectangle);
            imageView.setImageResource(R.drawable.recyclerview_item_top_rectangle_selector);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) imageView.getLayoutParams();
            layoutParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            layoutParams.height = parent.getResources().getDimensionPixelSize(R.dimen.recycler_item_triangle_height);
            imageView.setLayoutParams(layoutParams);
            return new ViewHolder(v);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_padding_sender,
                    parent, false);
            view.setBackgroundResource(R.drawable.sender_recyclerview_item_background_selector2);
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) view.getLayoutParams();
            layoutParams.width = paddingWidth;
            view.setLayoutParams(layoutParams);
            return new ViewHolder(view);
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
            viewHolder.itemTextView.setText(CoinUtil.valueString(item.account.getCurrencyBasedBalance().confirmed.getValue()
                    , denomination, false) + " " + denomination.getUnicodeName());
            viewHolder.valueTextView.setText(item.account.getReceivingAddress().get().toString());
        }
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
            categoryTextView = (TextView) v.findViewById(R.id.categorytextView);
            itemTextView = (TextView) v.findViewById(R.id.itemTextView);
            valueTextView = (TextView) v.findViewById(R.id.valueTextView);
        }
    }
}
