package com.mycelium.wallet.activity.main.adapter;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.main.model.ActionButton;

import java.util.ArrayList;
import java.util.List;

public class ButtonAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int SPACE = 10;
    private static final int BUTTON = 1;
    private List<ActionButton> buttons = new ArrayList<>();

    private ButtonClickListener clickListener;

    public void setButtons(List<ActionButton> buttons) {
        boolean update = true;
        if (buttons.equals(this.buttons)) {
            update = false;
        }
        this.buttons.clear();
        this.buttons.addAll(buttons);
        if (update) {
            notifyDataSetChanged();
        }
    }

    public void setClickListener(ButtonClickListener clickListener) {
        this.clickListener = clickListener;
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
            button.setText(actionButton.getText());
            if (actionButton.getIcon() != 0) {
                button.setCompoundDrawablesWithIntrinsicBounds(actionButton.getIcon(), 0, 0, 0);
            } else if (actionButton.getIconUrl() != null) {
                int size = button.getResources().getDimensionPixelSize(R.dimen.button_ads_icon_size);
                Glide.with(button.getContext())
                        .load(actionButton.getIconUrl())
                        .into(new SimpleTarget<Drawable>(size, size) {
                            @Override
                            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                button.setCompoundDrawablesWithIntrinsicBounds(resource, null, null, null);
                            }
                        });
            } else {
                button.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
            button.setTextColor(actionButton.getTextColor() != 0 ?
                    actionButton.getTextColor() : button.getResources().getColor(R.color.fio_white_alpha_0_8));
            button.setPadding(button.getResources().getDimensionPixelSize(actionButton.getIcon() != 0 ?
                            R.dimen.page_margin_width : R.dimen.button_padding_large)
                    , button.getPaddingTop(), button.getPaddingRight(), button.getPaddingBottom());
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (clickListener != null) {
                        clickListener.onClick(actionButton);
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
                text = buttons.get(1).getText();
            } else {
                text = buttons.get(buttons.size() - 2).getText();
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
            button = itemView.findViewById(R.id.btn_action);
        }
    }

    private class SpaceHolder extends RecyclerView.ViewHolder {
        public SpaceHolder(View itemView) {
            super(itemView);
        }
    }
}
