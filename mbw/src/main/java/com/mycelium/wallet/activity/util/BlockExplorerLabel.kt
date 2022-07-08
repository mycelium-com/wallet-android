package com.mycelium.wallet.activity.util

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import com.google.common.base.Strings
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.external.partner.openLink

abstract class BlockExplorerLabel : AppCompatTextView {
    private fun init() {
        this.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        this.typeface = Typeface.MONOSPACE
    }

    constructor(context: Context?) : super(context!!) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context!!, attrs, defStyleAttr) {
        init()
    }

    protected abstract val linkText: String?
    protected abstract val formattedLinkText: String?
    protected abstract fun getLinkURL(blockExplorer: BlockExplorer?): String?

    fun update_ui() {
        if (Strings.isNullOrEmpty(linkText)) {
            super.setText("")
        } else {
            val link = SpannableString(formattedLinkText)
            link.setSpan(UnderlineSpan(), 0, link.length, 0)
            this.text = link
            this.setTextColor(resources.getColor(R.color.brightblue))
        }
    }

    protected fun setHandler(blockExplorer: BlockExplorer?) {
        if (!Strings.isNullOrEmpty(linkText)) {
            setOnLongClickListener {
                Utils.setClipboardString(linkText, this@BlockExplorerLabel.context)
                Toaster(context).toast(R.string.copied_to_clipboard, true)
                true
            }
            setOnClickListener {
                Toaster(context).toast(R.string.redirecting_to_block_explorer, true)
                context.openLink(getLinkURL(blockExplorer))
            }
        }
    }
}