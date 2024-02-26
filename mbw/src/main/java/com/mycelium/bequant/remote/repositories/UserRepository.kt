package com.mycelium.bequant.remote.repositories

import android.app.Activity
import androidx.core.content.edit
import com.mycelium.bequant.BequantConstants
import com.mycelium.bequant.remote.model.User
import com.mycelium.wallet.WalletApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

class UserRepository {
    private val _userFlow = MutableStateFlow<User?>(null)
    val userFlow = _userFlow.filterNotNull()

    // todo mock
    private val preference by lazy {
        WalletApplication.getInstance().getSharedPreferences(
            BequantConstants.PUBLIC_REPOSITORY, Activity.MODE_PRIVATE
        )
    }

    suspend fun identify() {
        // todo mock
        delay(10000)
        val isVIP = preference.getBoolean("VIP3", false)
        _userFlow.value = User(status = if (isVIP) User.Status.VIP else User.Status.REGULAR)
    }

    suspend fun applyVIPCode(code: String): User.Status {
        // todo mock
        delay(2000)
        if (code.length == 8) throw Exception("Code length 8 is prohibited")
        val success = code.length > 6
        preference.edit { putBoolean("VIP3", success) }
        val status = if (success) User.Status.VIP else User.Status.REGULAR
        _userFlow.value = User(status = status)
        return status
    }
}