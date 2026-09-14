package com.mycelium.wallet.activity.deprecated

import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.masterseed.MasterSeedManager
import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger
import java.util.UUID

/**
 * Bitcoin Vault and FIO were removed from the wallet. Migration 13.sqm keeps a snapshot of the
 * accounts a user had in the DeprecatedAccount table so we can warn about the ones that may still
 * hold funds and hand out the keys needed to spend them elsewhere.
 */
data class DeprecatedAccount(
    val uuid: UUID,
    val currencyId: String,
    val label: String,
    val accountIndex: Int,
    val accountType: Int,
    val confirmed: BigInteger,
) {
    val coin: DeprecatedCoin = DeprecatedCoin.of(currencyId)

    val hasFunds: Boolean
        get() = confirmed.signum() > 0

    /** Only master-seed derived accounts have keys we can still derive. */
    val isFromMasterSeed: Boolean
        get() = accountType == ACCOUNT_TYPE_FROM_MASTERSEED

    fun formattedBalance(): String =
        BigDecimal(confirmed).movePointLeft(coin.decimals).stripTrailingZeros().toPlainString() +
                " " + coin.symbol

    companion object {
        const val ACCOUNT_TYPE_FROM_MASTERSEED = 0
    }
}

enum class DeprecatedCoin(
    val id: String,
    val coinName: String,
    val symbol: String,
    val decimals: Int,
    val bip44CoinType: Int,
) {
    BTCV_MAIN("bitcoinvault.main", "Bitcoin Vault", "BTCV", 8, 440),
    BTCV_TEST("bitcoinvault.test", "Bitcoin Vault Test", "tBTCV", 8, 441),
    FIO_MAIN("fio.main", "FIO", "FIO", 9, 235),
    FIO_TEST("fio.test", "FIO test", "FIO", 9, 235);

    val isFio: Boolean
        get() = this == FIO_MAIN || this == FIO_TEST

    companion object {
        fun of(id: String): DeprecatedCoin =
            values().firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("Unknown deprecated currency $id")
    }
}

/** One exportable secret of a deprecated account. */
data class DeprecatedAccountKey(val derivationPath: String, val key: String)

object DeprecatedAccounts {
    /**
     * Accounts the user still has to be told about: everything that was not acknowledged yet and
     * either holds a confirmed balance or is not recoverable from the backup words on its own.
     */
    fun pending(walletDB: WalletDB): List<DeprecatedAccount> =
        walletDB.deprecatedAccountQueries.selectNotAcknowledged().executeAsList()
            .mapNotNull { row ->
                val currency = DeprecatedCoin.values().firstOrNull { it.id == row.currency }
                    ?: return@mapNotNull null
                DeprecatedAccount(
                    uuid = row.uuid,
                    currencyId = currency.id,
                    label = row.accountName,
                    accountIndex = row.accountIndex,
                    accountType = row.accountType,
                    confirmed = confirmedBalance(row.balance),
                )
            }
            .filter { it.hasFunds }

    fun acknowledge(walletDB: WalletDB) {
        walletDB.deprecatedAccountQueries.acknowledgeAll()
    }

    /**
     * The balance column holds the raw JSON the Balance adapter used to write, which we can no
     * longer decode with that adapter: its coin is gone from COINS.
     */
    private fun confirmedBalance(raw: String): BigInteger = try {
        JSONObject(raw).getJSONArray("Balance").getString(0).toBigInteger()
    } catch (e: Exception) {
        BigInteger.ZERO
    }

    /**
     * Re-derives the keys of a deprecated account from the master seed. FIO signs with a single
     * private key per account, Bitcoin Vault is a Bitcoin fork and needs the account xPriv of every
     * derivation scheme it used.
     */
    fun keysOf(
        account: DeprecatedAccount,
        masterSeedManager: MasterSeedManager,
        network: NetworkParameters,
    ): List<DeprecatedAccountKey> {
        if (!account.isFromMasterSeed) {
            return emptyList()
        }
        val seed = masterSeedManager.getMasterSeed(AesKeyCipher.defaultKeyCipher()).bip32Seed
        val coin = account.coin
        return if (coin.isFio) {
            // FioKeyManager derived m/44'/235'/index'/0/0 and signed with that key directly.
            val key = HdKeyNode.fromSeed(seed, BipDerivationType.BIP44)
                .createChildNode(HdKeyPath.valueOf("m/44'/${coin.bip44CoinType}'"))
                .createHardenedChildNode(account.accountIndex)
                .createChildNode(0)
                .createChildNode(0)
                .privateKey
            listOf(
                DeprecatedAccountKey(
                    "m/44'/${coin.bip44CoinType}'/${account.accountIndex}'/0/0",
                    key.getBase58EncodedPrivateKey(network)
                )
            )
        } else {
            listOf(BipDerivationType.BIP44, BipDerivationType.BIP49, BipDerivationType.BIP84)
                .map { derivation ->
                    val accountRoot = HdKeyNode.fromSeed(seed, derivation)
                        .createChildNode(derivation.getHardenedPurpose())
                        .createHardenedChildNode(coin.bip44CoinType)
                        .createChildNode(account.accountIndex or HARDENED)
                    DeprecatedAccountKey(
                        "m/${derivation.purpose}'/${coin.bip44CoinType}'/${account.accountIndex}'",
                        accountRoot.serialize(network, derivation)
                    )
                }
        }
    }

    private const val HARDENED = -0x80000000
}
