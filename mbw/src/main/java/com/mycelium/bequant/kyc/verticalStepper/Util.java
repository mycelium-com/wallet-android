package com.mycelium.bequant.kyc.verticalStepper;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.TypedValue;

class Util {
    static float dpToPx(Context context, float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics());
    }

    static int getThemeColor(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(
                typedValue.data,
                new int[]{attr});
        int color = a.getColor(0, 0);
        a.recycle();
        return color;
    }
}
