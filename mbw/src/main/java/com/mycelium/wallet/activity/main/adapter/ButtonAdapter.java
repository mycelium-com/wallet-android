package com.mycelium.wallet.activity.main.adapter;

import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.main.model.ActionButton;

import java.util.ArrayList;
import java.util.List;

public class ButtonAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int SPACE = 10;
    private static final int BUTTON = 1;
    private List<ActionButton> buttons = new ArrayList<>();

    public void setButtons(List<ActionButton> buttons) {
        this.buttons.clear();
        this.buttons.addAll(buttons);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == BUTTON) {
            return new ButtonHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_action_button, parent, false));
        } else {
            return new SpaceHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_space, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == BUTTON) {
            final ActionButton actionButton = buttons.get(position);
            Button button = ((ButtonHolder) holder).button;
            button.setText(actionButton.text);
            button.setCompoundDrawablesWithIntrinsicBounds(actionButton.icon, 0, 0, 0);
            if (actionButton.textColor != 0) {
                button.setTextColor(actionButton.textColor);
            } else {
                button.setTextColor(button.getResources().getColor(R.color.btn_text_color));
            }
            if (actionButton.icon != 0) {
                button.setPadding(button.getResources().getDimensionPixelSize(R.dimen.button_padding)
                        , button.getPaddingTop(), button.getPaddingRight(), button.getPaddingBottom());
            } else {
                button.setPadding(button.getResources().getDimensionPixelSize(R.dimen.button_padding_large)
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
        } else {
            Paint paint = new Paint();
            float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                    17, holder.itemView.getResources().getDisplayMetrics());
            paint.setTextSize(textSize);
            String text;
            if (position == 0) {
                text = buttons.get(1).text;
            } else {
                text = buttons.get(buttons.size() - 2).text;
            }
            int width = (int) paint.measureText(text);

            int paddings = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    58, holder.itemView.getResources().getDisplayMetrics());
            ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
            layoutParams.width = (holder.itemView.getResources().getDisplayMetrics().widthPixels - width - paddings) / 2;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (buttons.get(position) == null) {
            return SPACE;
        } else {
            return BUTTON;
        }
    }

    @Override
    public int getItemCount() {
        return buttons.size();
    }

    public static class ButtonHolder extends RecyclerView.ViewHolder {
        Button button;

        public ButtonHolder(View itemView) {
            super(itemView);
            button = (Button) itemView.findViewById(R.id.btn_action);
        }
    }

    private class SpaceHolder extends RecyclerView.ViewHolder {
        public SpaceHolder(View itemView) {
            super(itemView);
        }
    }
}
