package com.mycelium.wallet.activity.rmc.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.LinearLayout;

public class ProfitMeterView extends LinearLayout {
    private Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int angle;

    public ProfitMeterView(Context context) {
        super(context);
    }

    public ProfitMeterView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ProfitMeterView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    {
        circlePaint.setColor(Color.parseColor("#787878"));
        circlePaint.setStyle(Paint.Style.STROKE);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        circlePaint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, dm));

        arcPaint.setColor(Color.parseColor("#fe8838"));
        arcPaint.setStyle(Paint.Style.STROKE);
        DisplayMetrics dm1 = getResources().getDisplayMetrics();
        arcPaint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, dm1));

        setWillNotDraw(false);
    }

    private int h;
    private int w;
    private RectF ovalRect = new RectF();

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        h = b - t - 6;
        w = r - l;
        ovalRect.left = (w - h) / 2;
        ovalRect.top = 3;
        ovalRect.right = h + ovalRect.left;
        ovalRect.bottom = h + 3;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(w / 2, h / 2 + 3, h / 2, circlePaint);
        canvas.drawArc(ovalRect, -90, angle, false, arcPaint);
    }

    public void setAngle(int angle) {
        this.angle = angle;
        postInvalidate();
    }
}
