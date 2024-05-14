package com.mycelium.wallet.external.changelly2.remote

import android.app.Activity
import androidx.core.content.edit
import com.mycelium.bequant.remote.model.UserStatus
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.external.vip.VipRetrofitFactory
import com.mycelium.wallet.external.vip.model.ActivateVipRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatusRepository {
    private val vipApi by lazy { VipRetrofitFactory().createApi() }
    private val preference = WalletApplication.getInstance()
        .getSharedPreferences(PREFERENCES_VIP_FILE, Activity.MODE_PRIVATE)

    private fun getLocalStatus() = UserStatus.fromName(preference.getString(VIP_STATUS_KEY, null))

    private val _statusFlow = MutableStateFlow(getLocalStatus() ?: UserStatus.REGULAR)
    val statusFlow = _statusFlow.asStateFlow()

    init {
        val localStatus = getLocalStatus()
        if (localStatus == null) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val checkResult = vipApi.check()
                    // if user is VIP than response contains his code else response contains empty string
                    val isVIP = checkResult.vipCode.isNotEmpty()
                    val status = if (isVIP) UserStatus.VIP else UserStatus.REGULAR
                    preference.edit { putString(VIP_STATUS_KEY, status.name) }
                    _statusFlow.value = status
                } catch (_: Exception) {
                }
            }
        }
    }

    suspend fun applyVIPCode(code: String): UserStatus {
        val response = vipApi.activate(ActivateVipRequest(code))
        val status = if (response.done) UserStatus.VIP else UserStatus.REGULAR
        _statusFlow.value = status
        preference.edit { putString(VIP_STATUS_KEY, status.name) }
        return status
    }

    private companion object {
        const val PREFERENCES_VIP_FILE = "VIP_PREFERENCES"
        const val VIP_STATUS_KEY = "VIP_STATUS"
    }
}
