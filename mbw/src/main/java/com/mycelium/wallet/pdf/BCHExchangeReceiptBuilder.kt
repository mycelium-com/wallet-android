package com.mycelium.wallet.pdf


import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import crl.android.pdfwriter.PaperSize
import java.io.OutputStream


private var pageInfo : PdfDocument.PageInfo? = null

class BCHExchangeReceiptBuilder {
    private var transactionId: String? = null
    private var date: String? = null
    private var spendingAmount: String? = null
    private var receivingAmount: String? = null
    private var receivingAddress: String? = null
    private var spendingAccountLabel: String? = null
    private var receivingAccountLabel: String? = null

    fun setTransactionId(transactionId: String) = apply { this.transactionId = transactionId }

    fun setDate(date: String) = apply { this.date = date }

    fun setSpendingAmount(spendingAmount: String) = apply { this.spendingAmount = spendingAmount }

    fun setReceivingAmount(receiving: String) = apply { this.receivingAmount = receiving }

    fun setReceivingAddress(receivingAddress: String) = apply { this.receivingAddress = receivingAddress }

    fun setSpendingAccountLabel(spendingAccountLabel: String) = apply { this.spendingAccountLabel = spendingAccountLabel }

    fun setReceivingAccountLabel(receivingAccountLabel: String) = apply { this.receivingAccountLabel = receivingAccountLabel }

    private val leftMargin = 48F

    fun build(stream: OutputStream) {
        val document = PdfDocument()


        // create a page description
        pageInfo = PdfDocument.PageInfo.Builder(PaperSize.A4_WIDTH, PaperSize.A4_HEIGHT, 1)
                .create()
        // start a page
        val page = document.startPage(pageInfo)
        val paint = TextPaint()

        val robotoRegular = Typeface.create("sans-serif", Typeface.NORMAL)
        val robotoLight = Typeface.create("sans-serif-light", Typeface.NORMAL)

        //Creating header
        paint.typeface = robotoRegular
        paint.textSize = 16F
        drawTextOnCanvasWithMagnifier(page.canvas, "Exchange confirmation", 200F, 102F, paint)

        //Creating body
        paint.textSize = 14F
        var height = 204F
        val dateTimeText = "Date and time: "
        drawTextOnCanvasWithMagnifier(page.canvas, dateTimeText, leftMargin, height, paint)
        paint.typeface = robotoLight
        drawTextOnCanvasWithMagnifier(page.canvas, date, leftMargin + paint.measureText(dateTimeText), height, paint)

        height += 17F
        paint.typeface = robotoRegular
        val exchangeDirectionText = "Exchange operation: "
        drawTextOnCanvasWithMagnifier(page.canvas, exchangeDirectionText, leftMargin, height, paint)
        paint.typeface = robotoLight
        drawTextOnCanvasWithMagnifier(page.canvas, "Bitcoin Cash (BCH) to Bitcoin (BTC)", leftMargin + paint.measureText(exchangeDirectionText), height, paint)

        height += 36F
        paint.typeface = robotoRegular
        drawTextOnCanvasWithMagnifier(page.canvas, "Blockchain Transaction ID:", leftMargin, height, paint)
        height += 18F
        paint.typeface = robotoLight
        paint.color = Color.parseColor("#5aa7e6")
        drawTextOnCanvasWithMagnifier(page.canvas, transactionId, leftMargin, height, paint)

        height += 36F
        paint.typeface = robotoRegular
        paint.color = Color.BLACK
        val sendingAccountText = "Exchanging account: "
        drawTextOnCanvasWithMagnifier(page.canvas, sendingAccountText, leftMargin, height, paint)
        paint.typeface = robotoLight
        drawTextOnCanvasWithMagnifier(page.canvas, spendingAccountLabel, leftMargin + paint.measureText(sendingAccountText), height, paint)

        height += 18F
        paint.typeface = robotoRegular
        val sendingAmountText = "Exchanging amount: "
        drawTextOnCanvasWithMagnifier(page.canvas, sendingAmountText, leftMargin, height, paint)
        paint.typeface = robotoLight
        drawTextOnCanvasWithMagnifier(page.canvas, spendingAmount, leftMargin + paint.measureText(sendingAmountText), height, paint)

        height += 36F
        paint.typeface = robotoRegular
        paint.color = Color.BLACK
        val receivingAccountText = "Receiving account: "
        drawTextOnCanvasWithMagnifier(page.canvas, receivingAccountText, leftMargin, height, paint)
        paint.typeface = robotoLight
        drawTextOnCanvasWithMagnifier(page.canvas, receivingAccountLabel, leftMargin + paint.measureText(receivingAccountText), height, paint)

        height += 18F
        paint.typeface = robotoRegular
        val receivingAddressText = "Receiving address: "
        drawTextOnCanvasWithMagnifier(page.canvas, receivingAddressText, leftMargin, height, paint)
        paint.typeface = robotoLight
        paint.color = Color.parseColor("#5aa7e6")
        drawTextOnCanvasWithMagnifier(page.canvas, receivingAddress, leftMargin + paint.measureText(receivingAddressText), height, paint)

        height += 18F
        paint.typeface = robotoRegular
        paint.color = Color.BLACK
        val receivingAmountText = "Receiving amount: "
        drawTextOnCanvasWithMagnifier(page.canvas, receivingAmountText, leftMargin, height, paint)
        paint.typeface = robotoLight
        drawTextOnCanvasWithMagnifier(page.canvas, receivingAmount, leftMargin + paint.measureText(receivingAmountText), height, paint)

        height += 18F
        paint.color = Color.parseColor("#fb746d")
        drawTextOnCanvasWithMagnifier(page.canvas, "Exchange rate is approximate due to the high volatility of the cryptomarket", leftMargin, height, paint)

        height += 36F
        paint.color = Color.BLACK
        drawTextOnCanvasWithMagnifier(page.canvas, "BTC will be sent after this transaction gets 12 confirmations.", leftMargin, height, paint)
        document.finishPage(page)
        document.writeTo(stream)
        document.close()
    }

    /**
     * Workaround for https://issuetracker.google.com/issues/36960285
     */
    private fun drawTextOnCanvasWithMagnifier(canvas: Canvas, text: String?, x: Float, y: Float, paint: Paint) {
        //workaround
        val originalTextSize = paint.textSize
        val magnifier = 1000f
        canvas.save()
        canvas.scale(1f / magnifier, 1f / magnifier)
        paint.textSize = originalTextSize * magnifier
        canvas.drawText(text, x * magnifier, y * magnifier, paint)
        canvas.restore()
        paint.textSize = originalTextSize
    }
}
