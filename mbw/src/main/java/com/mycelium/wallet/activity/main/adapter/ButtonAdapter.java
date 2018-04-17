package com.mycelium.wallet.activity.main.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.main.model.ActionButton;

import java.util.ArrayList;
import java.util.List;

public class ButtonAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<ActionButton> buttons = new ArrayList<>();

    public void setButtons(List<ActionButton> buttons) {
        this.buttons = buttons;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ButtonHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_action_button, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final ActionButton actionButton = buttons.get(position);
        Button button = ((ButtonHolder) holder).button;
        button.setText(actionButton.text);
        button.setCompoundDrawablesRelativeWithIntrinsicBounds(actionButton.icon, 0, 0, 0);
        if (buttons.get(position).icon != 0) {
            button.setPadding(button.getResources().getDimensionPixelSize(R.dimen.button_padding)
                    , button.getPaddingTop(), button.getPaddingRight(), button.getPaddingBottom());
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (actionButton.task != null) {
                    actionButton.task.run();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return buttons.size();
    }

    public static class ButtonHolder extends RecyclerView.ViewHolder {
        Button button;

        public ButtonHolder(View itemView) {
            super(itemView);
            button = itemView.findViewById(R.id.btn_action);
        }
    }
}
