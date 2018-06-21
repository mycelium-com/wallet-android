package com.mycelium.wallet.activity.settings.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.TextView;

import com.mycelium.wallet.R;
import com.mycelium.wapi.api.lib.CurrencyCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocalCurrencyAdapter extends ArrayAdapter<CurrencyCode> {
    private LayoutInflater inflater;
    private List<CurrencyCode> selected;
    private List<CurrencyCode> data;

    public LocalCurrencyAdapter(@NonNull Context context, @NonNull List<CurrencyCode> objects) {
        super(context, 0, new ArrayList<>(objects));
        inflater = LayoutInflater.from(context);
        data = objects;
    }

    public List<CurrencyCode> getSelected() {
        return selected;
    }

    public void setSelected(List<CurrencyCode> selected) {
        this.selected = selected;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int pos, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.listview_item_with_checkbox, null);
        }
        CurrencyCode currencyCode = getItem(pos);

        TextView tv = convertView.findViewById(R.id.tv_currency_name);
        tv.setText(currencyCode.getName());
        TextView tvShort = convertView.findViewById(R.id.tv_currency_short);
        tvShort.setText(currencyCode.getShortString());

        CheckBox box = convertView.findViewById(R.id.checkbox_currency);
        box.setChecked(selected.contains(currencyCode));

        return convertView;
    }

    public void toggleChecked(CurrencyCode currencyCode) {
        if (selected.contains(currencyCode)) {
            selected.remove(currencyCode);
        } else {
            selected.add(currencyCode);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return filter;
    }

    private Filter filter = new Filter() {
        @Override
        protected Filter.FilterResults performFiltering(CharSequence constraint) {
            String filterString = constraint.toString().toLowerCase();

            Filter.FilterResults results = new Filter.FilterResults();

            List<CurrencyCode> resultList = new ArrayList<>();

            for (CurrencyCode currencyCode : data) {
                if (currencyCode.getName().toLowerCase().contains(filterString)
                        || currencyCode.getShortString().toLowerCase().contains(filterString)) {
                    resultList.add(currencyCode);
                }
            }
            results.values = resultList;
            results.count = resultList.size();

            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, Filter.FilterResults filterResults) {
            clear();
            if(filterResults.values != null) {
                addAll((List<CurrencyCode>) filterResults.values);
            }
            notifyDataSetChanged();
        }
    };
}
