package com.mycelium.wallet.activity.view;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;

import com.mycelium.wallet.R;

public class OnOffPreference extends Preference {
    private String widgetText;

    public OnOffPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_on_off);
    }

    public OnOffPreference(Context context) {
        super(context);
        setWidgetLayoutResource(R.layout.preference_on_off);
    }

    public void setWidgetText(String widgetText) {
        this.widgetText = widgetText;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView textView = holder.itemView.findViewById(R.id.on_off_text);
        textView.setText(widgetText);
    }
}
