package com.mycelium.wallet.activity.view

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.SearchView

/**
 * SearchView for a `collapseActionView` menu item whose Back key always collapses it once the keyboard is hidden.
 *
 * The stock query field consumes Back in onKeyPreIme only to clear its focus, so after the IME closed itself on
 * the 1st Back, the 2nd Back did nothing visible and only the 3rd collapsed the search. Skipping the pre-IME
 * stage for Back lets a shown IME still hide itself first, and otherwise the key reaches the activity, which
 * collapses the expanded action view (AppCompat's onBackPressed) and on the next Back leaves the screen.
 */
class BackCollapsingSearchView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    SearchView(context, attrs) {
    override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean =
        if (event.keyCode == KeyEvent.KEYCODE_BACK) false else super.dispatchKeyEventPreIme(event)
}
