package com.mycelium.bequant.kyc.verticalStepper;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.mycelium.wallet.R;

public class VerticalStepperItemCircleView extends FrameLayout {
    private TextView number;

    private ImageView icon;

    public VerticalStepperItemCircleView(Context context) {
        super(context);
        initialize(context);
    }

    public VerticalStepperItemCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public VerticalStepperItemCircleView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public VerticalStepperItemCircleView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initialize(Context context) {
        LayoutInflater.from(context).inflate(
                R.layout.vertical_stepper_view_item_circle,
                this,
                true);

        number = (TextView) findViewById(R.id.vertical_stepper_view_item_circle_number);
        icon = (ImageView) findViewById(R.id.vertical_stepper_view_item_circle_icon);
    }

    public void setBackgroundActive() {
        GradientDrawable drawable = (GradientDrawable) ContextCompat
                .getDrawable(
                        getContext(),
                        R.drawable.vertical_stepper_view_item_circle_active);
        drawable.setColor(Util
                .getThemeColor(getContext(), R.attr.colorAccent));
        setBackgroundResource(R.drawable.vertical_stepper_view_item_circle_active);
    }

    public void setBackgroundInactive() {
        setBackgroundResource(R.drawable.vertical_stepper_view_item_circle_inactive);
    }

    public void setNumber(int value) {
        icon.setVisibility(View.GONE);
        number.setVisibility(View.VISIBLE);
        number.setText(String.valueOf(value));
    }

    public void setIconCheck() {
        setIconResource(R.drawable.ic_vertical_step);
    }

    public void setIconEdit() {
        setIconResource(R.drawable.ic_vertical_step);
    }

    public void setIconResource(int id) {
        number.setVisibility(View.GONE);
        icon.setVisibility(View.VISIBLE);
        icon.setImageResource(id);
    }
}
