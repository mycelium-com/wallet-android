package com.mycelium.wallet

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

inline fun startCoroutineTimer(
    scope: CoroutineScope,
    delayMillis: Long = 0,
    repeatMillis: Long = 0,
    crossinline action: () -> Unit
) = scope.launch {
    delay(delayMillis)
    if (repeatMillis > 0) {
        while (true) {
            action()
            delay(repeatMillis)
        }
    } else {
        action()
    }
}