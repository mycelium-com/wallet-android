package com.mycelium.wallet.activity.main.model;

import android.graphics.drawable.Drawable;

public class RecommendationBanner extends RecommendationInfo {
    public static final int BANNER_TYPE = 3;

    public Drawable banner;

    public RecommendationBanner(Drawable banner) {
        super(BANNER_TYPE);
        this.banner = banner;
    }
}
