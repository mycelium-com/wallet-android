package com.mycelium.wapi.wallet

object LoadingProgressTracker {
    val subscribers: MutableList<LoadingProgressUpdater> = ArrayList()

    fun setStatus(status: LoadingProgressStatus) = subscribers.forEach { it.status = status }

    fun setPercent(percent: Int) = subscribers.forEach { it.percent = percent }

    fun subscribe(tracker: LoadingProgressUpdater) = subscribers.add(tracker)

    fun clearLastFullUpdateTime() = subscribers.forEach(LoadingProgressUpdater::clearLastFullUpdateTime)
}