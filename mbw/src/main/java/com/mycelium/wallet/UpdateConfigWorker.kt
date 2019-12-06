package com.mycelium.wallet
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.mycelium.wallet.WorkerUtils.hide
import com.mycelium.wallet.WorkerUtils.makeStatusNotification

class UpdateConfigWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        makeStatusNotification("Updating", applicationContext)
        return try {
            MbwManager.getInstance(applicationContext).updateConfig()
            hide(applicationContext)
            Result.success()
        } catch (throwable: Throwable) {
            Result.failure()
        }
    }
}