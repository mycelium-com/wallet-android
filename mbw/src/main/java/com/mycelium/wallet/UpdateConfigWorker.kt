package com.mycelium.wallet

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class UpdateConfigWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        return try {
            MbwManager.getInstance(applicationContext).updateConfig()
            Result.success()
        } catch (throwable: Throwable) {
            Result.failure()
        }
    }
}