package com.mycelium.wallet

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

private const val WORK_NAME_PERIODIC = "configupdate-periodic"

fun start(context: Context) {
    val workManager = WorkManager.getInstance(context);
    val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    val workRequest = PeriodicWorkRequest.Builder(UpdateConfigWorker::class.java, Constants.CONFIG_UPDATE_PERIOD_MINS, TimeUnit.MINUTES)
            .setInitialDelay(Constants.CONFIG_UPDATE_PERIOD_MINS, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
    workManager.enqueueUniquePeriodicWork(WORK_NAME_PERIODIC, ExistingPeriodicWorkPolicy.REPLACE, workRequest);
}

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