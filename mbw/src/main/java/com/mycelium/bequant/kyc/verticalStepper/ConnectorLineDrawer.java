package com.mycelium.bequant.kyc.verticalStepper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import androidx.core.content.res.ResourcesCompat;

import com.mycelium.wallet.R;

class ConnectorLineDrawer {
    private final Paint paint = new Paint();

    private final RectF line = new RectF();

    ConnectorLineDrawer(Context context) {
        int grey = ResourcesCompat.getColor(
                context.getResources(),
                R.color.bequant_green,
                null);
        paint.setColor(grey);
    }

    void adjust(Context context, int width, int height) {
        line.left = Util.dpToPx(context, 19.5f);
        line.right = Util.dpToPx(context, 20.5f);
        line.top = Util.dpToPx(context, 40);
        line.bottom = height;
    }

    void draw(Canvas canvas) {
        canvas.drawRect(line, paint);
    }
}
