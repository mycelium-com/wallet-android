package com.mycelium.wallet

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

inline fun startCoroutineTimer(
    scope: CoroutineScope,
    delayMillis: Long = 0,
    repeatMillis: Long = 0,
    crossinline action: (Int) -> Unit
) = scope.launch {
    delay(delayMillis)
    var counter = 0
    if (repeatMillis > 0) {
        while (true) {
            action(counter++)
            delay(repeatMillis)
        }
    } else {
        action(counter)
    }
}

fun <E> List<E>.randomOrNull(): E? = if (size > 0) random() else null