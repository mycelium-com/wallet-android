package com.mycelium.wallet.activity.settings.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.mycelium.wallet.R;

import java.util.ArrayList;
import java.util.List;

public class DialogListAdapter extends RecyclerView.Adapter<DialogListAdapter.ViewHolder> {
    private int selected;
    private List<String> data = new ArrayList<>();
    private ClickListener clickListener;

    public DialogListAdapter(CharSequence[] data, int selected) {
        this.selected = selected;
        for (CharSequence datum : data) {
            this.data.add(datum.toString());
        }
    }

    public void setSelected(int selected) {
        int old = this.selected;
        this.selected = selected;
        notifyItemChanged(old);
        notifyItemChanged(selected);
    }

    public int getSelected() {
        return selected;
    }

    @SuppressWarnings("unused") // TODO: use this to save the user clicks
    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.dialog_list_checked_button, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final String item = data.get(position);
        holder.checkedTextView.setText(item);
        holder.checkedTextView.setOnCheckedChangeListener(null);
        holder.checkedTextView.setChecked(position == selected);
        holder.checkedTextView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton compoundButton, boolean b) {
                if (b) {
                    final int pos = holder.getAdapterPosition();
                    setSelected(pos);
                    if (clickListener != null) {
                        compoundButton.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                clickListener.onClick(data.get(pos), pos);
                            }
                        }, 200); // need to user see click reaction
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        AppCompatRadioButton checkedTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            checkedTextView = itemView.findViewById(R.id.checkbox);
        }
    }

    public interface ClickListener {
        void onClick(String value, int position);
    }
}
