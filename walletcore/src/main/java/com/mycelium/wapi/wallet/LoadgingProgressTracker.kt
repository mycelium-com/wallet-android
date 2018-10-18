package com.mycelium.wapi.wallet

object LoadingProgressTracker {
    val subscribers: MutableList<LoadingProgressUpdater> = ArrayList()

    fun setComment(comment: String) = subscribers.forEach { it.comment = comment }

    fun setPercent(percent: Int) = subscribers.forEach { it.percent = percent }

    fun subscribe(tracker: LoadingProgressUpdater) = subscribers.add(tracker)

    fun clearLastFullUpdateTime() = subscribers.forEach(LoadingProgressUpdater::clearLastFullUpdateTime)
}