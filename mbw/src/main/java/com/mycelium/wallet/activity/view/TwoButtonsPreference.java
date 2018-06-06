package com.mycelium.wallet.activity.view;


import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.View;
import android.widget.Button;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.settings.ModulePreference;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TwoButtonsPreference extends Preference implements ModulePreference {
    @BindView(R.id.top_button)
    Button topButton;

    @BindView(R.id.bottom_button)
    Button bottomButton;

    private View.OnClickListener topButtonClickListener;
    private View.OnClickListener bottomButtonClickListener;
    private String topButtonText;
    private String bottomButtonText;
    private boolean topButtonEnabled;
    private boolean bottomButtonEnabled;

    public TwoButtonsPreference(Context context) {
        super(context);
        setWidgetLayoutResource(R.layout.two_button_preference);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        ButterKnife.bind(this, holder.itemView);
        topButton.setText(topButtonText);
        bottomButton.setText(bottomButtonText);
        topButton.setEnabled(topButtonEnabled);
        bottomButton.setEnabled(bottomButtonEnabled);
        topButton.setOnClickListener(topButtonClickListener);
        bottomButton.setOnClickListener(bottomButtonClickListener);
    }

    public void setTopButtonClickListener(View.OnClickListener buttonClickListener) {
        topButtonClickListener = buttonClickListener;
    }

    public void setBottomButtonClickListener(View.OnClickListener buttonClickListener) {
        bottomButtonClickListener = buttonClickListener;
    }

    public void setEnabled(boolean preferenceEnabled, boolean topButtonEnabled, boolean bottomButtonEnabled) {
        this.topButtonEnabled = topButtonEnabled;
        this.bottomButtonEnabled = bottomButtonEnabled;
        this.setEnabled(preferenceEnabled);
        if (topButton != null) {
            topButton.setEnabled(topButtonEnabled);
        }
        if (bottomButton != null) {
            bottomButton.setEnabled(bottomButtonEnabled);
        }
    }

    public void setButtonsText(String topButtonText, String bottomButtonText) {
        this.bottomButtonText = bottomButtonText;
        this.topButtonText = topButtonText;

        if (bottomButton != null) {
            bottomButton.setText(bottomButtonText);
        }

        if (topButton != null) {
            topButton.setText(topButtonText);
        }
    }

    @Override
    public void setSyncStateText(String syncStatus) {

    }

    @Override
    public void setUnderIconText(String underIconText) {

    }
}
