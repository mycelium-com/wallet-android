package com.mycelium.wallet.activity.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by elvis on 07.09.17.
 */

public class AnimationUtils {

    private static final long DURATION = 400;

    public static void collapse(final View view, final Runnable end) {
        final ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        ValueAnimator anim = ValueAnimator.ofInt(view.getHeight(), 0);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                layoutParams.height = (int) valueAnimator.getAnimatedValue();
                view.setLayoutParams(layoutParams);
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                view.setVisibility(View.GONE);
                if (end != null) {
                    end.run();
                }
            }
        });
        anim.setDuration(DURATION);
        anim.start();
    }

    public static void expand(final View view, final Runnable end) {
        view.setVisibility(View.VISIBLE);
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(view.getResources().getDisplayMetrics().widthPixels, View.MeasureSpec.AT_MOST);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthMeasureSpec, heightMeasureSpec);
        int height = view.getMeasuredHeight();

        final ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        ValueAnimator anim = ValueAnimator.ofInt(0, height);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                layoutParams.height = (int) valueAnimator.getAnimatedValue();
                view.setLayoutParams(layoutParams);
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (end != null) {
                    end.run();
                }
            }
        });
        anim.setDuration(DURATION);
        anim.start();
    }

}
