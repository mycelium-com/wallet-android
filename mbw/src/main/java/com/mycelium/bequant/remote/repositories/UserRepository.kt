package com.mycelium.bequant.remote.repositories

import com.mycelium.bequant.remote.model.User
import com.mycelium.wallet.external.vip.VipRetrofitFactory
import com.mycelium.wallet.external.vip.model.ActivateVipRequest
import com.mycelium.wallet.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

class UserRepository {
    private val _userFlow = MutableStateFlow<User?>(null)
    private val vipApi = VipRetrofitFactory().createApi()
    val userFlow = _userFlow.filterNotNull()

    suspend fun identify() {
        try {
            val checkResult = vipApi.check()
            // if user is VIP than response contains his code else response contains empty string
            val isVIP = checkResult.vipCode.isNotEmpty()
            val status = if (isVIP) User.Status.VIP else User.Status.REGULAR
            _userFlow.update { user -> user?.copy(status = status) ?: User(status) }
        } catch (_: Exception) {
            _userFlow.value = User()
        }
    }

    suspend fun applyVIPCode(code: String): User.Status {
        val response = vipApi.activate(ActivateVipRequest(code))
        val status = if (response.done) User.Status.VIP else User.Status.REGULAR
        _userFlow.update { user -> user?.copy(status = status) ?: User(status) }
        return status
    }
}
