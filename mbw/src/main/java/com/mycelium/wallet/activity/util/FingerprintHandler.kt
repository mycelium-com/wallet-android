package com.mycelium.wallet.activity.util

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties.*
import androidx.annotation.RequiresApi
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.core.os.CancellationSignal
import com.mycelium.wallet.R
import java.security.KeyStore
import java.security.KeyStoreException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey


class FingerprintHandler {

    private val cancelSignal = CancellationSignal()

    @RequiresApi(Build.VERSION_CODES.M)
    fun startAuth(context: Context, success: () -> Unit, fail: (String) -> Unit) {
        if (isFingerprintAvailable(context)) {
            generateKey()?.let {
                val cipher = Cipher.getInstance("$KEY_ALGORITHM_AES/$BLOCK_MODE_CBC/$ENCRYPTION_PADDING_PKCS7")
                it.load(null)
                val key = it.getKey("key", null) as SecretKey
                cipher.init(Cipher.ENCRYPT_MODE, key)

                val cryptoObject = FingerprintManagerCompat.CryptoObject(cipher)
                val fingerprintManagerCompat = FingerprintManagerCompat.from(context)
                fingerprintManagerCompat.authenticate(cryptoObject, 0, cancelSignal,
                        Callback(context, success, fail), null)
            }
        }
    }

    fun cancelAuth() {
        cancelSignal.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun generateKey(): KeyStore? {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore?.load(null)

            val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM_AES, "AndroidKeyStore")
            keyGenerator.init(KeyGenParameterSpec.Builder("key", PURPOSE_ENCRYPT.or(PURPOSE_DECRYPT))
                    .setBlockModes(BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(ENCRYPTION_PADDING_PKCS7)
                    .build())

            keyGenerator.generateKey()
            return keyStore
        } catch (exc: KeyStoreException) {
            exc.printStackTrace()
        }
        return null
    }

    class Callback(val context: Context,
                   val success: () -> Unit,
                   private val fail: (String) -> Unit) : FingerprintManagerCompat.AuthenticationCallback() {
        override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
            super.onAuthenticationError(errMsgId, errString)
            fail.invoke(errString.toString())
        }

        override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)
            success.invoke()
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            fail.invoke(context.getString(R.string.fingerprint_detection_failed))
        }
    }

    companion object {
        /*
        * Condition II: Check if the device has fingerprint sensors.
        * Note: If you marked android.hardware.fingerprint as something that
        * your app requires (android:required="true"), then you don't need
        * to perform this check.
        *
        * */
        @JvmStatic
        fun isHardwareSupported(context: Context): Boolean =
                FingerprintManagerCompat.from(context).isHardwareDetected

        /*
        * Condition III: Fingerprint authentication can be matched with a
        * registered fingerprint of the user. So we need to perform this check
        * in order to enable fingerprint authentication
        * */
        @JvmStatic
        fun isFingerprintAvailable(context: Context): Boolean =
                FingerprintManagerCompat.from(context).hasEnrolledFingerprints()
    }
}
