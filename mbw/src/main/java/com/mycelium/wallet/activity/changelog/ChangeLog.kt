package com.mycelium.wallet.activity.changelog

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mycelium.wallet.activity.changelog.datasource.ChangeLogDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

internal object ChangeLog {

    /**
     * Show the [Change Log] popup.
     */
    fun show(fragmentManager: FragmentManager, tag: String? = null): BottomSheetDialogFragment =
        ChangeLogBottomSheetDialogFragment().also { it.show(fragmentManager, tag) }

    /**
     * Show the [Change Log] bottom sheet in case when the app has been updated and change logs exist for the new
     * version.
     *
     * @return [BottomSheetDialogFragment] if displayed, otherwise [null].
     */
    suspend fun showIfNewVersion(
        context: Context,
        fragmentManager: FragmentManager,
        tag: String? = null
    ): BottomSheetDialogFragment? {
        val hasNewReleaseChangeLog = ChangeLogDataSource(context.applicationContext).hasNewReleaseChangeLog()
        return if (
            hasNewReleaseChangeLog &&
            !fragmentManager.isStateSaved &&
            !fragmentManager.isDestroyed
        ) {
            withContext(Dispatchers.Main.immediate) {
                if (isActive) show(fragmentManager, tag) else null
            }
        } else null
    }
}
