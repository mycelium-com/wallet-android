package com.mycelium.wallet.activity.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.mycelium.wallet.R
import com.mycelium.wallet.WalletApplication


class FingerprintHandler : BiometricPrompt.AuthenticationCallback() {
    private var biometricPrompt: BiometricPrompt? = null
    var successListener: (() -> Unit)? = null
    var failListener: ((String) -> Unit)? = null

    fun onCreate(activity: FragmentActivity) {
        biometricPrompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), this)
    }

    fun authenticate(context: Context): Boolean {
        if (canAuthWithBiometric(context)) {
            biometricPrompt?.authenticate(BiometricPrompt.PromptInfo.Builder()
                    .setTitle(context.getString(R.string.unlock_label_fingerprint))
                    .setConfirmationRequired(false)
                    .setNegativeButtonText(context.getString(R.string.use_pin))
                    .build())
            return true
        }
        return false
    }

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        failListener?.invoke(errString.toString())
    }

    override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        failListener?.invoke(WalletApplication.getInstance().getString(R.string.fingerprint_not_recognized))
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        successListener?.invoke()
    }

    companion object {
        @JvmStatic
        fun canAuthWithBiometric(context: Context): Boolean =
                BiometricManager.from(context)
                        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    }
}
