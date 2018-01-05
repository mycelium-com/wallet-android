package com.mycelium.wallet.activity.view;


import android.content.Context;
import android.preference.Preference;
import android.view.View;
import android.view.ViewGroup;
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

    public ButtonPreference(Context context) {
        super(context);
        setWidgetLayoutResource(R.layout.preference_button);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        ButterKnife.bind(this, view);
        view.setClickable(false); // disable parent click
        return view;
    }

    @Override
    protected void onBindView(final View view) {
        super.onBindView(view);
        button.setText(buttonText);
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
}
