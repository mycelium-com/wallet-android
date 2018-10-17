package com.mycelium.wallet

import android.content.Context
import android.os.Handler
import com.mycelium.wallet.event.MigrationCommentChanged
import com.mycelium.wallet.event.MigrationPercentChanged
import com.mycelium.wallet.persistence.MetadataStorage
import com.mycelium.wapi.wallet.LoadingProgressUpdater

class LoadingProgressTracker(val context: Context) : LoadingProgressUpdater {
    val eventBus = MbwManager.getEventBus()!!

    override var comment = ""
        set(value) {
            Handler(context.mainLooper).post {
                eventBus.post(MigrationCommentChanged(value))
            }
            field = value
        }
    override var percent = 0
        set(value) {
            Handler(context.mainLooper).post {
                eventBus.post(MigrationPercentChanged(value))
            }
            field = value
        }


    override fun clearLastFullUpdateTime() {
        MetadataStorage(context).setLastFullSync(0)
    }
}
