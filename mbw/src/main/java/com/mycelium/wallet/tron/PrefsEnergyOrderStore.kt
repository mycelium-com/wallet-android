package com.mycelium.wallet.tron

import android.content.SharedPreferences
import com.mycelium.wapi.wallet.tron.energy.EnergyOrderInfo
import com.mycelium.wapi.wallet.tron.energy.EnergyOrderStore
import java.util.UUID

/**
 * Per-account marker of an energy-service order handed to the partner and not yet terminal,
 * stored as `orderId|accountId|createdAt|mainTxId|mainTxExpiration`.
 */
class PrefsEnergyOrderStore(private val prefs: SharedPreferences) : EnergyOrderStore {
    override fun getPendingOrder(accountId: UUID): EnergyOrderInfo? {
        val raw = prefs.getString(key(accountId), null) ?: return null
        val parts = raw.split("|")
        if (parts.size < 3) return null
        return try {
            EnergyOrderInfo(
                orderId = parts[0],
                accountId = UUID.fromString(parts[1]),
                createdAtMs = parts[2].toLong(),
                mainTxId = parts.getOrNull(3)?.takeIf { it.isNotEmpty() },
                mainTxExpirationMs = parts.getOrNull(4)?.toLongOrNull()?.takeIf { it > 0 },
            )
        } catch (e: Exception) {
            null
        }
    }

    override fun setPendingOrder(order: EnergyOrderInfo) {
        prefs.edit().putString(key(order.accountId),
            "${order.orderId}|${order.accountId}|${order.createdAtMs}|${order.mainTxId ?: ""}|${order.mainTxExpirationMs ?: 0}").apply()
    }

    override fun clearPendingOrder(accountId: UUID) {
        prefs.edit().remove(key(accountId)).apply()
    }

    private fun key(accountId: UUID) = "pending_energy_order_$accountId"
}
