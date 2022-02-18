package com.mycelium.giftbox.purchase

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

fun <T> LiveData<T>.debounce(duration: Long = 1000L) = MediatorLiveData<T>().also { mld ->
  val source = this
  val handler = Handler(Looper.getMainLooper())

  val runnable = Runnable {
    mld.value = source.value
  }

  mld.addSource(source) {
    handler.removeCallbacks(runnable)
    handler.postDelayed(runnable, duration)
  }
}