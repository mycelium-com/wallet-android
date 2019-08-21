package com.mycelium.wallet.activity.util

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.core.os.CancellationSignal
import java.security.KeyStore
import java.security.KeyStoreException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey


class FingerprintHandler {

    private val cancelSignal = CancellationSignal()
    private var keyStore: KeyStore? = null

    @RequiresApi(Build.VERSION_CODES.M)
    fun startAuth(context: Context, success: () -> Unit, fail: (String) -> Unit) {
        generateKey()
        val cipher = Cipher.getInstance(
                KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7)

        keyStore?.load(null)
        val key = keyStore?.getKey("key", null) as SecretKey
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val cryptoObject = FingerprintManagerCompat.CryptoObject(cipher)

        val fingerprintManagerCompat = FingerprintManagerCompat.from(context)
        fingerprintManagerCompat.authenticate(cryptoObject, 0, cancelSignal, Callback(success, fail), null)
    }

    fun cancelAuth() {
        cancelSignal.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun generateKey() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore?.load(null);

            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyGenerator.init(KeyGenParameterSpec.Builder("key", KeyProperties.PURPOSE_ENCRYPT.or(KeyProperties.PURPOSE_DECRYPT))
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build())

            keyGenerator.generateKey();

        } catch (exc: KeyStoreException) {
            exc.printStackTrace();
        }
    }

    class Callback(val success: () -> Unit, val fail: (String) -> Unit) : FingerprintManagerCompat.AuthenticationCallback() {
        override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
            super.onAuthenticationError(errMsgId, errString)
            fail.invoke(errString.toString())
        }

        override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)
            success.invoke()
        }

        override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence?) {
            super.onAuthenticationHelp(helpMsgId, helpString)
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            fail.invoke("Fingerprint detection is failed")
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
        fun isHardwareSupported(context: Context): Boolean {
            val fingerprintManager = FingerprintManagerCompat.from(context)
            return fingerprintManager.isHardwareDetected
        }

        /*
        * Condition III: Fingerprint authentication can be matched with a
        * registered fingerprint of the user. So we need to perform this check
        * in order to enable fingerprint authentication
        * */
        @JvmStatic
        fun isFingerprintAvailable(context: Context): Boolean {
            val fingerprintManager = FingerprintManagerCompat.from(context)
            return fingerprintManager.hasEnrolledFingerprints()
        }
    }
}