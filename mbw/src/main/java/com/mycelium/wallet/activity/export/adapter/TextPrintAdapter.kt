package com.mycelium.wallet.activity.export.adapter

import android.content.Context
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import java.io.FileOutputStream

class TextPrintAdapter(
    private val context: Context,
    private val pdfDocument: String
) : PrintDocumentAdapter() {

    override fun onStart() {
        // Called before printing starts
    }

    override fun onLayout(
        oldAttributes: android.print.PrintAttributes,
        newAttributes: android.print.PrintAttributes,
        cancellationSignal: android.os.CancellationSignal,
        callback: LayoutResultCallback,
        extras: android.os.Bundle?
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder("print_output.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()

        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<android.print.PageRange>,
        destination: android.os.ParcelFileDescriptor,
        cancellationSignal: android.os.CancellationSignal,
        callback: WriteResultCallback
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onWriteCancelled()
            return
        }
        val stream = FileOutputStream(destination.fileDescriptor)
        stream.write(pdfDocument.toByteArray())
        stream.close()
        callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
    }
}
