package com.mycelium.wallet.activity.view;


import android.content.Context;
import android.preference.Preference;
import android.view.View;
import android.widget.Button;

import com.mycelium.wallet.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ButtonPreference extends Preference {
    @BindView(R.id.preference_button)
    Button button;

    private View.OnClickListener buttonClickListener;
    private String buttonText;
    private boolean buttonEnabled = true;

    public ButtonPreference(Context context) {
        super(context);
        setWidgetLayoutResource(R.layout.preference_button);
    }

    @Override
    protected void onBindView(final View view) {
        super.onBindView(view);
        ButterKnife.bind(this, view);
        button.setText(buttonText);
        setButtonEnabled(buttonEnabled);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (button != null) {
            button.setEnabled(enabled);
        }
    }

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

    public void setButtonEnabled(boolean enabled) {
        buttonEnabled = enabled;
        if (button != null) {
            button.setClickable(enabled);
            button.setEnabled(enabled);
        }
    }
}
