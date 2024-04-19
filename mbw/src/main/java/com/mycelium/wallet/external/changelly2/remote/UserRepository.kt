package com.mycelium.wallet.external.changelly2.remote

import android.app.Activity
import androidx.core.content.edit
import com.mycelium.bequant.remote.model.User
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.external.vip.VipRetrofitFactory
import com.mycelium.wallet.external.vip.model.ActivateVipRequest
import com.mycelium.wallet.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

class UserRepository {
    private val _userFlow = MutableStateFlow<User?>(null)
    private val vipApi = VipRetrofitFactory().createApi()
    private val preference by lazy {
        WalletApplication
            .getInstance()
            .getSharedPreferences(PREFERENCES_VIP_FILE, Activity.MODE_PRIVATE)
    }

    val userFlow = _userFlow.filterNotNull()

    suspend fun identify() {
        val status = try {
            val checkResult = vipApi.check()
            // if user is VIP than response contains his code else response contains empty string
            val isVIP = checkResult.vipCode.isNotEmpty()
            if (isVIP) User.Status.VIP
            else User.Status.REGULAR
        } catch (_: Exception) {
            val statusName = preference.getString(VIP_STATUS_KEY, null)
            if (statusName != null) User.Status.fromName(statusName)
            else User.Status.REGULAR
        }
        _userFlow.value = User(status)
        preference.edit { putString(VIP_STATUS_KEY, status.name) }
    }

    suspend fun applyVIPCode(code: String): User.Status {
        val response = vipApi.activate(ActivateVipRequest(code))
        val status = if (response.done) User.Status.VIP else User.Status.REGULAR
        _userFlow.update { user -> user?.copy(status = status) ?: User(status) }
        preference.edit { putString(VIP_STATUS_KEY, status.name) }
        return status
    }

    private companion object {
        const val PREFERENCES_VIP_FILE = "VIP_PREFERENCES"
        const val VIP_STATUS_KEY = "VIP_STATUS"
    }
}
