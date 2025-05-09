package com.mycelium.wallet

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.GsonBuilder
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.BitcoinAddress
import com.mycelium.view.Denomination
import com.mycelium.view.Denomination.Companion.fromString
import com.mycelium.wallet.lt.LocalTraderManager
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.btc.BtcWalletManagerBacking
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.bip44.TaprootMigrationHDAccountConfig
import com.mycelium.wapi.wallet.btc.bip44.getActiveMasterseedHDAccounts
import com.mycelium.wapi.wallet.btc.single.PrivateSingleConfig
import com.mycelium.wapi.wallet.btc.single.PublicSingleConfig
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccountContext
import java.util.Collections
import java.util.UUID

/**
 * One time migration that require more than what is at hands on the DB level can be
 * migrated here.
 */
class MbwMigration(
    val backing: BtcWalletManagerBacking<SingleAddressAccountContext>,
    val preferences: SharedPreferences,
    val exchangeDataPreferences: SharedPreferences,
    val dataPreferences: SharedPreferences,
    val selectedPreferences: SharedPreferences,
    val walletManager: WalletManager,
    val environment: MbwEnvironment,
    val localTraderManager: LocalTraderManager,
    val selectAccount: (UUID) -> Unit
) {
    fun migrate() {
        val fromVersion = preferences.getInt("upToDateVersion", 0)
        if (fromVersion < 20021) {
            migrateOldKeys()
        }
        if (fromVersion < 2120029) {
            // set default address type to P2PKH for uncompressed SA accounts
            walletManager.getAccountIds().forEach { accountId ->
                val account = walletManager.getAccount(accountId)
                if (account is SingleAddressAccount) {
                    val pubKey = account.getPublicKey()
                    if (pubKey != null && !pubKey.isCompressed) {
                        account.setDefaultAddressType(AddressType.P2PKH)
                    }
                }
            }
        }
        if (fromVersion < 3030000) {
            migratePreferences()
        }
        if (fromVersion < 3190001) {
            migrateTaproot()
        }
        preferences.edit { putInt("upToDateVersion", 3190000) }
    }

    fun migrateTaproot() {
        walletManager.getActiveMasterseedHDAccounts().filterIsInstance<HDAccount>().forEach { account ->
            walletManager.createAccounts(TaprootMigrationHDAccountConfig(account))
        }
    }

    /**
     * put previously single values into map-like values where key is a coin type
     * i.e. "mbtc" -> {"Bitcoin": "mbtc"}
     */
    private fun migratePreferences() {
        val gson = GsonBuilder().create()

        // Miner fee
        val oldFee: String? = preferences.getString(Constants.MINER_FEE_SETTING, null)
        if (oldFee != null) {
            val newFee: MutableMap<String?, MinerFee?> = HashMap<String?, MinerFee?>()
            newFee.put(Utils.getBtcCoinType().getName(), MinerFee.fromString(oldFee))
            preferences.edit { putString(Constants.MINER_FEE_SETTING, gson.toJson(newFee)) }
        }

        // Block explorer
        val oldExplorer: String? = preferences.getString("BlockExplorer", null)
        if (oldExplorer != null) {
            val newExplorer: MutableMap<String?, String?> = HashMap<String?, String?>()
            for (entry in environment.getBlockExplorerMap().entries) {
                newExplorer.put(entry.key, entry.value[0].identifier)
            }
            newExplorer.put(Utils.getBtcCoinType().getName(), oldExplorer)
            preferences.edit {
                remove("BlockExplorer") // don't use old name anymore
                putString(Constants.BLOCK_EXPLORERS, gson.toJson(newExplorer))
            }
        }

        // Denomination
        val oldDenomination: String? = preferences.getString("BitcoinDenomination", null)
        if (oldDenomination != null) {
            val newDenomination: MutableMap<String?, Denomination?> = HashMap<String?, Denomination?>()
            newDenomination.put(Utils.getBtcCoinType().getName(), fromString(oldDenomination))
            preferences.edit {
                remove("BitcoinDenomination") // don't use old name anymore
                putString(Constants.DENOMINATION_SETTING, gson.toJson(newDenomination))
            }
        }

        // Exchange rates source (different preference file)
        val oldSource = exchangeDataPreferences.getString(Constants.EXCHANGE_RATE_SETTING, null)
        if (oldSource != null) {
            val newSource: MutableMap<String?, String?> = HashMap<String?, String?>()
            newSource.put(Utils.getBtcCoinType().getSymbol(), oldSource)
            exchangeDataPreferences.edit { putString(Constants.EXCHANGE_RATE_SETTING, gson.toJson(newSource)) }
        }
    }

    private fun migrateOldKeys() {
        // We only migrate old keys if we don't have any accounts yet - otherwise, migration has already taken place
        if (!walletManager.getAccountIds().isEmpty()) {
            return
        }

        // Get the local trader address, may be null
        val localTraderAddress: BitcoinAddress? = localTraderManager.localTraderAddress
        if (localTraderAddress == null) {
            localTraderManager.unsetLocalTraderAccount()
        }

        //check which address was the last recently selected one
        val lastAddress: String = selectedPreferences.getString("last", null)!!

        // Migrate all existing records to accounts
        val records: MutableList<Record> = loadClassicRecords()

        records.forEach { record ->
            // Create an account from this record
            val account =
                if (record.hasPrivateKey()) {
                    walletManager.createAccounts(PrivateSingleConfig(record.key, AesKeyCipher.defaultKeyCipher()))
                        .firstOrNull()
                } else {
                    walletManager.createAccounts(PublicSingleConfig(record.key.publicKey)).firstOrNull()
                }

            //check whether this was the selected record
            if (account != null && record.address.toString() == lastAddress) {
                selectAccount(account)
            }

            //check whether the record was archived
            if (account != null && record.tag == Record.Tag.ARCHIVE) {
                walletManager.getAccount(account)?.archiveAccount()
            }

            // See if we need to migrate this account to local trader
            if (BitcoinAddress.fromString(record.address.toString()) == localTraderAddress) {
                if (record.hasPrivateKey()) {
                    localTraderManager.setLocalTraderData(
                        account, record.key, BitcoinAddress.fromString(record.address.toString()),
                        localTraderManager.nickname
                    )
                } else {
                    localTraderManager.unsetLocalTraderAccount()
                }
            }
        }
    }

    private fun loadClassicRecords(): MutableList<Record> {
        val recordList = mutableListOf<Record>()

        // Load records
        val records: String = dataPreferences.getString("records", "")!!
        for (one in records.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            var one: String? = one
            one = one!!.trim { it <= ' ' }
            if (one.isEmpty()) {
                continue
            }
            val record = Record.fromSerializedString(one)
            if (record != null) {
                recordList.add(record)
            }
        }

        // Sort all records
        Collections.sort<Record?>(recordList)
        return recordList
    }
}
