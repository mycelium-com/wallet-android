package com.poovam.pinedittextfield;

import android.content.res.Resources;


public class Util {
    public static float dpToPx(float dp) {
        return (dp * Resources.getSystem().getDisplayMetrics().density);
    }
}