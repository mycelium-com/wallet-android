package com.mycelium.wallet.external.changelly;


import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mycelium.wallet.R;

import java.util.ArrayList;

public class CurrenciesAdapter extends ArrayAdapter<CurrencyInfo> {

    Context context;
    int layoutResourceId;
    ArrayList<CurrencyInfo> data = null;
    ClickListener clickListener;

    public CurrenciesAdapter(Context context, int layoutResourceId, ArrayList<CurrencyInfo> data) {
        super(context, layoutResourceId, data);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.data = data;
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ItemHolder holder;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new ItemHolder();
            holder.imgIcon = (ImageView) row.findViewById(R.id.ivIcon);
            holder.txtName = (TextView) row.findViewById(R.id.tvTitle);
            row.setTag(holder);
        } else {
            holder = (ItemHolder) row.getTag();
        }

        final CurrencyInfo bean = data.get(position);
        holder.txtName.setText(bean.getName());
        holder.imgIcon.setImageResource(bean.getSmallIcon());

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(clickListener != null) {
                    clickListener.itemClick(bean);
                }
            }
        });
        return row;
    }

    @Override
    public int getCount() {

        if(data.size()<=0)
            return 1;
        return data.size();
    }

    @Override
    public CurrencyInfo getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public static class ItemHolder {
        public ImageView imgIcon;
        public TextView txtName;
        public TextView txtDescription;
    }

    public interface ClickListener {
        void itemClick(CurrencyInfo info);
    }
}
