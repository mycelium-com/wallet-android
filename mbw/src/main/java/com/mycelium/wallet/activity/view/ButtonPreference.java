package com.mycelium.wallet.activity.view;


import android.content.Context;
import android.preference.Preference;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;

import com.mycelium.wallet.R;

public class ButtonPreference extends Preference {

    public ButtonPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWidgetLayoutResource(R.layout.preference_button);
    }

    public ButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_button);
    }

    public ButtonPreference(Context context) {
        super(context);
        setWidgetLayoutResource(R.layout.preference_button);
    }

    @Override
    protected void onBindView(final View view) {
        super.onBindView(view);
        view.setClickable(false); // disable parent click
        View button = view.findViewById(R.id.btn_install);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // persist your value here
                if (buttonClickListener != null) {
                    buttonClickListener.onClick(v);
                }
            }
        });
    }



    private View.OnClickListener buttonClickListener;

    public void setButtonClickListener(View.OnClickListener buttonClickListener) {
        this.buttonClickListener = buttonClickListener;
    }

}
