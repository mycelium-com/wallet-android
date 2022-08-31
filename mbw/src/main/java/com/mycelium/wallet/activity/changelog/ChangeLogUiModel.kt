package com.mycelium.wallet.activity.changelog

internal sealed class ChangeLogUiModel(val type: Int) {

    class LatestRelease(val versionCode: Int, val version: String) : ChangeLogUiModel(VIEW_TYPE) {

        companion object {
            const val VIEW_TYPE = 0
        }
    }

    class Release(val versionCode: Int, val version: String) : ChangeLogUiModel(VIEW_TYPE) {

        companion object {
            const val VIEW_TYPE = 1
        }
    }

    class Change(val change: String) : ChangeLogUiModel(VIEW_TYPE) {

        companion object {
            const val VIEW_TYPE = 2
        }
    }
}
