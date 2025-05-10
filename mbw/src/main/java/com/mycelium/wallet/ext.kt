package com.mycelium.wallet

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

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

fun <E> List<E>.randomOrNull(): E? = if (isNotEmpty()) random() else null

/**
 * Updates the [MutableStateFlow.value] atomically using the specified [function] of its value.
 *
 * [function] may be evaluated multiple times, if [value] is being concurrently updated.
 */
inline fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
    while (true) {
        val prevValue = value
        val nextValue = function(prevValue)
        if (compareAndSet(prevValue, nextValue)) {
            return
        }
    }
}

fun Context.checkPushPermission(run: () -> Unit, noPermission: () -> Unit = {}) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                this,
                POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            run()
        } else {
            noPermission()
        }
    } else {
        run()
    }
}

fun OkHttpClient.Builder.configureSSLSocket(): OkHttpClient.Builder {
    val sslContext = SSLContext.getInstance("TLS")

    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    trustManagerFactory.init(null as KeyStore?)
    val trustManagers = trustManagerFactory.trustManagers
    val trustManager = trustManagers.first { it is X509TrustManager } as X509TrustManager

    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
        sslContext.init(null, arrayOf(trustManager), null)
    } else {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )
        sslContext.init(null, trustAllCerts, null)
    }
    this.sslSocketFactory(sslContext.socketFactory, trustManager)
    return this
}