package com.mycelium.bequant.kyc.verticalStepper;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.mycelium.wallet.R;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class VerticalStepperItemView extends FrameLayout {
    public static int STATE_INACTIVE = 0;

    public static int STATE_ACTIVE = 1;

    public static int STATE_COMPLETE = 2;

    private boolean showConnectorLine = true;

    private boolean editable = false;

    private VerticalStepperItemCircleView circle;

    private int number;

    private LinearLayout wrapper;

    private TextView title;


    private ConnectorLineDrawer connector;

    private int state = STATE_INACTIVE;

    public VerticalStepperItemView(Context context) {
        super(context);
        initialize(context);
    }

    public VerticalStepperItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public VerticalStepperItemView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public VerticalStepperItemView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initialize(Context context) {
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);

        int padding = (int) Util.dpToPx(context, 8);
        setPadding(padding, padding, padding, 0);

        LayoutInflater.from(context).inflate(
                R.layout.vertical_stepper_view_item,
                this,
                true);

        circle = (VerticalStepperItemCircleView) findViewById(R.id.vertical_stepper_view_item_circle);
        wrapper = (LinearLayout) findViewById(R.id.vertical_stepper_view_item_wrapper);
        title = (TextView) findViewById(R.id.vertical_stepper_view_item_title);
        connector = new ConnectorLineDrawer(context);
    }

    public boolean getShowConnectorLine() {
        return showConnectorLine;
    }

    public void setShowConnectorLine(boolean show) {
        showConnectorLine = show;
        setMarginBottom(state == STATE_ACTIVE);
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;

        if (state == STATE_COMPLETE)
            if (isEditable())
                circle.setIconEdit();
            else
                circle.setIconCheck();
    }

    public void setCircleNumber(int number) {
        this.number = number;

        if (state != STATE_COMPLETE)
            circle.setNumber(number);
    }

    public void setTitle(CharSequence title) {
        this.title.setText(title);
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;

        if (state == STATE_INACTIVE)
            setStateInactive();
        else if (state == STATE_ACTIVE)
            setStateActive();
        else
            setStateComplete();
    }

    private void setStateInactive() {
        circle.setIconEdit();
        setMarginBottom(false);
        circle.setNumber(number);
        circle.setBackgroundInactive();
        title.setTextColor(ResourcesCompat.getColor(
                getResources(),
                R.color.bequant_gray_6,
                null));
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
    }

    private void setStateActive() {
        circle.setIconEdit();
        setMarginBottom(true);
        circle.setNumber(number);
        circle.setBackgroundActive();
        title.setTextColor(ResourcesCompat.getColor(
                getResources(),
                R.color.bequant_gray_6,
                null));
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
    }

    private void setStateComplete() {
        setMarginBottom(false);
        circle.setBackgroundActive();

        if (isEditable())
            circle.setIconEdit();
        else
            circle.setIconCheck();

        title.setTextColor(ResourcesCompat.getColor(
                getResources(),
                R.color.white,
                null));
        title.setTypeface(title.getTypeface(), Typeface.BOLD);

    }

    private void setMarginBottom(boolean active) {
        MarginLayoutParams params = (MarginLayoutParams) wrapper
                .getLayoutParams();

        if (!getShowConnectorLine())
            params.bottomMargin = 0;
        else if (active)
            params.bottomMargin = (int) Util.dpToPx(getContext(), 48);
        else
            params.bottomMargin = (int) Util.dpToPx(getContext(), 40);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (showConnectorLine)
            connector.draw(canvas);
    }

    @Override
    protected void onSizeChanged(
            int width,
            int height,
            int oldWidth,
            int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        connector.adjust(getContext(), width, height);
    }
}
