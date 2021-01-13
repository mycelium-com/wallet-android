package com.mycelium.bequant.common

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.core.view.drawToBitmap
import kotlin.math.roundToInt


object BlurBuilder {
    private const val BITMAP_SCALE = 0.4f
    private const val BLUR_RADIUS = 25f
    fun blur(activity: Activity): Bitmap {
        return blur(activity, getDimmedScreenshot(activity))
    }

    private fun blur(ctx: Context?, image: Bitmap): Bitmap {
        val width = (image.width * BITMAP_SCALE).roundToInt()
        val height = (image.height * BITMAP_SCALE).roundToInt()
        val inputBitmap = Bitmap.createScaledBitmap(image, width, height, false)
        val outputBitmap = Bitmap.createBitmap(inputBitmap)
        val rs = RenderScript.create(ctx)
        val theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        val tmpIn = Allocation.createFromBitmap(rs, inputBitmap)
        val tmpOut = Allocation.createFromBitmap(rs, outputBitmap)
        theIntrinsic.setRadius(BLUR_RADIUS)
        theIntrinsic.setInput(tmpIn)
        theIntrinsic.forEach(tmpOut)
        tmpOut.copyTo(outputBitmap)
        return outputBitmap
    }

    private fun getDimmedScreenshot(activity: Activity): Bitmap {
        val v = activity.window.decorView
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint()
        // to dim (darken) bitmap
        val filter: ColorFilter = LightingColorFilter(Color.parseColor("#FF4F4F4F"), 0)
        p.colorFilter = filter

        c.drawBitmap(v.drawToBitmap(), Matrix(), p)
        return b
    }
}