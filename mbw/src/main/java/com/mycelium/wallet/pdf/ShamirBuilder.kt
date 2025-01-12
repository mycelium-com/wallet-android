package com.mycelium.wallet.pdf

import android.content.Context
import android.util.Log
import com.mrd.bitlib.crypto.BipSss
import crl.android.pdfwriter.PaperSize
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class ShamirBuilder {

    var shares: List<BipSss.Share> = listOf()

    fun Int.pr(procent: Int): Int = this * procent / 100

    fun build(): String {
        val pageWidth = PaperSize.EXECUTIVE_WIDTH
        val pageHeight = PaperSize.EXECUTIVE_HEIGHT
        val MARGIN_START = pageWidth.pr(5)
        val writer = PdfWriter(pageWidth, pageHeight, 20, 20, 20, 20)
        var fromTop = 0

        writer.addText(MARGIN_START, fromTop, 16, "Shamir shares")
        fromTop += 20

        val now = Date()
        val usLocale = Locale("en_US")
        writer.addText(
            MARGIN_START, fromTop, 14,
            "Creation date: ${DateFormat.getDateInstance(DateFormat.LONG, usLocale).format(now)}"
        )
        fromTop += 20

        writer.addText(
            MARGIN_START,
            fromTop,
            14,
            "You need only ${shares.first().threshold} to restore the secret"
        )
        fromTop += 30

        shares.forEach {
            if (fromTop + pageHeight.pr(25) > pageHeight) {
                writer.addPage()
                fromTop = 0
            }
            writer.addText(MARGIN_START, fromTop, 18, "Share ${it.shareNumber}");
            fromTop += 20

            val data = it.toString()
            data.chunked(32).forEach { part ->
                writer.addText(MARGIN_START, fromTop, 14, part)
                fromTop += 21
            }

            fromTop += 10
            writer.addQrCode(2.0, fromTop * 27.0 / pageHeight, 3.5, data)
            fromTop += writer.translateCmX(4.1)
        }

        return writer.asString()
    }


    companion object {
        fun exportShamirSharesToFile(context: Context, params: ShamirBuilder, filePath: String) {
            val pdfString = params.build()
            try {
                val stream = context.openFileOutput(filePath, Context.MODE_PRIVATE);
                stream.write(pdfString.toByteArray(StandardCharsets.UTF_8));
                stream.close();
            } catch (e: IOException) {
                Log.e("ShamirBuilder", "IOException while writing file", e)
                throw e
            }
        }

    }
}