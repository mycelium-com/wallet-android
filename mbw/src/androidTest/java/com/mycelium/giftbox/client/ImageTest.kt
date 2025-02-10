package com.mycelium.giftbox.client

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.mrd.bitlib.crypto.Bip39
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.activity.AboutActivity
import com.mycelium.wapi.wallet.AesKeyCipher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageTest {
    val imagesRuna = listOf(
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

    val imagesCloudfront = listOf(
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b916708-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b521096-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b332558-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b248402-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b734388-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b916708-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b521096-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b332558-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b248402-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b734388-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b332558-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b295114-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b505744-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b010383-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b708501-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b276459-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b331047-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b341225-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b725361-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b140822-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b140822-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b140822-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b967988-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b656796-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b303523-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b904523-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b527371-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b330854-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b440379-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b538052-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b565734-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b719673-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b981697-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b583438-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b961769-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/B985731-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/B985731-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b557924-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b721470-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b998921-300w-326ppi.png",
        "https://d30s7yzk2az89n.cloudfront.net/images/brands/b076101-300w-326ppi.png",

        )

    private val WORD_LIST = arrayOf(
        "abandon", "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
        "abandon", "abandon", "abandon", "abandon", "about"
    )

    @Before
    fun setUp() {
        val mbwManager =
            MbwManager.getInstance(ApplicationProvider.getApplicationContext<Context>())
        if (!mbwManager.masterSeedManager.hasBip32MasterSeed()) {
            val masterSeed = Bip39.generateSeedFromWordList(WORD_LIST, "")
            mbwManager.masterSeedManager.configureBip32MasterSeed(
                masterSeed,
                AesKeyCipher.defaultKeyCipher()
            )
        }
    }

    @get:Rule
    val activityRule = ActivityScenarioRule(AboutActivity::class.java)

    internal class CustomTarget(val image: String) : SimpleTarget<Drawable>() {
        var timeStart: Long = 0
        var isEnded: Boolean = false
        override fun onLoadStarted(placeholder: Drawable?) {
            super.onLoadStarted(placeholder)
            Log.e("!!!", "onLoadStarted img=${image}")
            timeStart = System.currentTimeMillis()
        }

        override fun onLoadFailed(errorDrawable: Drawable?) {
            super.onLoadFailed(errorDrawable)
            isEnded = true
        }

        override fun onResourceReady(
            resource: Drawable,
            transition: Transition<in Drawable>?
        ) {
            isEnded = true
            Log.e("!!!", "loadTime time=${System.currentTimeMillis() - timeStart} img=${image}")
        }
    }

    @Test
    fun testRuna() {
        val glide = Glide.with(ApplicationProvider.getApplicationContext<Context>())
        val timeMap = mutableMapOf<String, Long>()
        var jobs = listOf<CustomTarget>()
        val scenario = activityRule.scenario
        scenario.onActivity { activity ->
            jobs = imagesRuna.map { image ->
                glide
                    .load(image)
                    .into(CustomTarget(image))
            }
        }
        do {
            if (jobs.all { it.isEnded }) {
                return
            }
        } while (true)
    }

    @Test
    fun testCloudfront() {
        val glide = Glide.with(ApplicationProvider.getApplicationContext<Context>())
        var jobs = listOf<CustomTarget>()
        val scenario = activityRule.scenario
        scenario.onActivity { activity ->
            jobs = imagesCloudfront.map { image ->
                glide
                    .load(image)
                    .into(CustomTarget(image))
            }
        }
        do {
            if (jobs.all { it.isEnded }) {
                return
            }
        } while (true)
    }
}