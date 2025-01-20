package com.mycelium.giftbox.client

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageTest {
    val res = listOf(
        "https://gift.runa.io/static/product_assets/MONOPX-FR/MONOPX-FR-card.png",
        "https://gift.runa.io/static/product_assets/800PET-US/800PET-US-card.png",
        "https://gift.runa.io/static/product_assets/BLUMEN-DE/BLUMEN-DE-card.png",
        "https://gift.runa.io/static/product_assets/1800BAS-US/1800BAS-US-card.png",
        "https://gift.runa.io/static/product_assets/1800FL-US/1800FL-US-card.png",
        "https://gift.runa.io/static/product_assets/76GAS-US/76GAS-US-card.png",
        "https://gift.runa.io/static/product_assets/ALDI-IE/ALDI-IE-card.png",
        "https://gift.runa.io/static/product_assets/ALI-IT/ALI-IT-card.png",
        "https://gift.runa.io/static/product_assets/ALZA-SK/ALZA-SK-card.png",
        "https://gift.runa.io/static/product_assets/AMPM-FR/AMPM-FR-card.png",
        "https://gift.runa.io/static/product_assets/AOSOM-FR/AOSOM-FR-card.png",
        "https://gift.runa.io/static/product_assets/APART-PL/APART-PL-card.png",
        "https://gift.runa.io/static/product_assets/ASGUES-FR/ASGUES-FR-card.png",
        "https://gift.runa.io/static/product_assets/ASDA-GB/ASDA-GB-card.png",
        "https://gift.runa.io/static/product_assets/ASKIT-GB/ASKIT-GB-card.png",
        "https://gift.runa.io/static/product_assets/ASOS/ASOS-card.png",
        "https://gift.runa.io/static/product_assets/ASOS/ASOS-card.png",
        "https://gift.runa.io/static/product_assets/ASOS/ASOS-card.png",
        "https://gift.runa.io/static/product_assets/ASOS-NL/ASOS-NL-card.png",
        "https://gift.runa.io/static/product_assets/ASOS-GB/ASOS-GB-card.png",
        "https://gift.runa.io/static/product_assets/ASOS-US/ASOS-US-card.png",
        "https://gift.runa.io/static/product_assets/ATTC-GR/ATTC-GR-card.png",
        "https://gift.runa.io/static/product_assets/ADTK-SK/ADTK-SK-card.png",
        "https://gift.runa.io/static/product_assets/ABBON-IT/ABBON-IT-card.png",
        "https://gift.runa.io/static/product_assets/ABRAR-FR/ABRAR-FR-card.png",
        "https://gift.runa.io/static/product_assets/PURLND-BE/PURLND-BE-card.png",
        "https://gift.runa.io/static/product_assets/PURLND-FR/PURLND-FR-card.png",
        "https://gift.runa.io/static/product_assets/ADAS-GR/ADAS-GR-card.png",
        "https://gift.runa.io/static/product_assets/ADAS-US/ADAS-US-card.png",
        "https://gift.runa.io/static/product_assets/AERIE-US/AERIE-US-card.png",
        "https://gift.runa.io/static/product_assets/AGRIT-IT/AGRIT-IT-card.png",
        "https://gift.runa.io/static/product_assets/AIRBB-CA/AIRBB-CA-card.png",
        "https://gift.runa.io/static/product_assets/AIRBNB-FR/AIRBNB-FR-card.png",
        "https://gift.runa.io/static/product_assets/AIRBNB-DE/AIRBNB-DE-card.png",
        "https://gift.runa.io/static/product_assets/AIRBNB-IT/AIRBNB-IT-card.png",
        "https://gift.runa.io/static/product_assets/AIRBNB-ES/AIRBNB-ES-card.png",
        "https://gift.runa.io/static/product_assets/AIRBNB-GB/AIRBNB-GB-card.png",
        "https://gift.runa.io/static/product_assets/AIRBNB-US/AIRBNB-US-card.png",
        "https://gift.runa.io/static/product_assets/AIRBB-AU/AIRBB-AU-card.png",
        "https://gift.runa.io/static/product_assets/AIRBNB-AT/AIRBNB-AT-card.png",
        "https://gift.runa.io/static/product_assets/AIRBNB-BE/AIRBNB-BE-card.png",
        "https://gift.runa.io/static/product_assets/AIRBNB-FI/AIRBNB-FI-card.png",
        "https://gift.runa.io/static/product_assets/AIRBNB-IE/AIRBNB-IE-card.png",
        "https://gift.runa.io/static/product_assets/AIRBNB-NL/AIRBNB-NL-card.png",
        "https://gift.runa.io/static/product_assets/AGIFT-AT/AGIFT-AT-card.png",
        "https://gift.runa.io/static/product_assets/AGIFT-BE/AGIFT-BE-card.png",
        "https://gift.runa.io/static/product_assets/AGIFT-CA/AGIFT-CA-card.png",
        "https://gift.runa.io/static/product_assets/AGIFT-EE/AGIFT-EE-card.png",
        "https://gift.runa.io/static/product_assets/AGIFT-FI/AGIFT-FI-card.png",
        "https://gift.runa.io/static/product_assets/AGIFT-FR/AGIFT-FR-card.png",
        "https://gift.runa.io/static/product_assets/AGIFT-DE/AGIFT-DE-card.png",
    )

    @Test
    public fun testList() {
        val glide = Glide.with(ApplicationProvider.getApplicationContext<Context>())
        val jobs = res.map { image ->
            Log.e("!!!", "start ${image}")
            glide
                .load(image)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable?>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("!!!", "onLoadFailed ${image}")
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable?>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("!!!", "onResourceReady ${image}")
                        return false
                    }
                })
                .into(250, (250 * 0.65f).toInt())
        }

        jobs.forEach {
        }
    }
}