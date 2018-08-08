package com.mycelium.wallet.activity.view;


import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.View;
import android.widget.TextView;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.settings.ModulePreference;

import javax.annotation.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;

public class ButtonPreference extends Preference implements ModulePreference {
    @BindView(R.id.preference_button)
    TextView button;

    @Nullable
    @BindView(R.id.under_icon_text)
    TextView underIconTextView;

    @Nullable
    @BindView(R.id.sync_state)
    TextView syncState;


    private View.OnClickListener buttonClickListener;
    private String buttonText;
    private String syncStateText;
    private boolean buttonEnabled = true;

    public ButtonPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_layout_no_icon);
        setWidgetLayoutResource(R.layout.preference_button);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        ButterKnife.bind(this, holder.itemView);
        if (button != null) {
            button.setText(buttonText);
        }
        if (syncState != null) {
            syncState.setText(syncStateText);
        }
        setButtonEnabled(buttonEnabled);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (button != null) {
            button.setEnabled(enabled);
        }
    }

    @Optional
    @OnClick(R.id.preference_button)
    void btnClick(View view) {
        if (buttonClickListener != null) {
            buttonClickListener.onClick(view);
        }
    }

    public void setButtonClickListener(View.OnClickListener buttonClickListener) {
        this.buttonClickListener = buttonClickListener;
    }

    public void setButtonText(String text) {
        buttonText = text;
        if (button != null) {
            button.setText(text);
        }
    }

    public void setSyncStateText(String syncStateText) {
        this.syncStateText = syncStateText;
        if (syncState != null) {
            syncState.setText(syncStateText);
        }
    }

    public void setButtonEnabled(boolean enabled) {
        buttonEnabled = enabled;
        if (button != null) {
            button.setClickable(enabled);
            button.setEnabled(enabled);
        }
    }
}
