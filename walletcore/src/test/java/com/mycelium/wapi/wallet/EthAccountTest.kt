package com.mycelium.wapi.wallet

import com.google.common.base.Optional
import com.mrd.bitlib.crypto.Bip39
import com.mrd.bitlib.crypto.RandomSource
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.WapiLogger
import com.mycelium.generated.wallet.database.AccountContext
import com.mycelium.generated.wallet.database.AccountContextQueries
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.btc.InMemoryBtcWalletManagerBacking
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.eth.EthAddress
import com.mycelium.wapi.wallet.eth.EthereumMasterseedConfig
import com.mycelium.wapi.wallet.eth.EthereumModule
import com.mycelium.wapi.wallet.exceptions.GenericInsufficientFundsException
import com.mycelium.wapi.wallet.genericdb.AccountContextsBacking
import com.mycelium.wapi.wallet.masterseed.MasterSeedManager
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.db.SqlCursor
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.infura.InfuraHttpService
import org.web3j.utils.Convert
import java.util.*


class EthAccountTest {
    private val MASTER_SEED_WORDS = "else tape female vast twist mandate lucky now license stand skull garment"
    private val web3j: Web3j = Web3j.build(InfuraHttpService("https://ropsten.infura.io/WKXR51My1g5Ea8Z5Xh3l"))
    private var account: EthAccount? = null

    @Suppress("UNCHECKED_CAST")
    @Before
    fun setup() {
        val fakeRandomSource = mock<RandomSource>(RandomSource::class.java)
        val fakeWapi = mock<Wapi>(Wapi::class.java)
        val fakeLogger = mock<WapiLogger>(WapiLogger::class.java)
        `when`<WapiLogger>(fakeWapi.logger).thenReturn(fakeLogger)

        val cursor = mock(SqlCursor::class.java)
        `when`(cursor.next()).thenReturn(false)
        val query = mock<Query<*>>(Query::class.java)
        `when`(query.execute()).thenReturn(cursor)
        val queries = mock(AccountContextQueries::class.java)
        `when`(queries.selectByUUID(any())).thenReturn(query as Query<AccountContext>)
        val db = mock(WalletDB::class.java)
        `when`(db.accountContextQueries).thenReturn(queries)

        val fakeMetadataStorage = mock<IMetaDataStorage>(IMetaDataStorage::class.java)
        `when`<Optional<String>>(fakeMetadataStorage.getFirstKeyForCategoryValue(any(String::class.java), any(String::class.java))).thenReturn(Optional.absent())
        `when`<String>(fakeMetadataStorage.getKeyCategoryValueEntry(any(String::class.java), any(String::class.java), any(String::class.java))).thenReturn("")

        val backing = InMemoryBtcWalletManagerBacking()
        val store = SecureKeyValueStore(backing, fakeRandomSource)
        val cipher = AesKeyCipher.defaultKeyCipher()

        val masterSeed = Bip39.generateSeedFromWordList(MASTER_SEED_WORDS.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(), "")

        val walletManager = WalletManager(NetworkParameters.testNetwork, fakeWapi,
                HashMap(), null, db)
        val listener = SynchronizeFinishedListener()
        walletManager.walletListener = listener
        walletManager.setIsNetworkConnected(true)

        val masterSeedManager = MasterSeedManager(store)
        masterSeedManager.configureBip32MasterSeed(masterSeed, cipher)

        val genericBacking = AccountContextsBacking(db)
        walletManager.add(EthereumModule(store, genericBacking, fakeMetadataStorage))

        val uuid = walletManager.createAccounts(EthereumMasterseedConfig())[0]
        account = walletManager.getAccount(uuid) as EthAccount

        // to update account's balance
        walletManager.startSynchronization()
        listener.waitForSyncFinished()
    }

    // some Kotlin/Mockito's business
    private fun <T> any(): T = Mockito.any<T>()

    @Test
    fun onfflineSigning() {
        val toAddress = EthAddress(account!!.coinType, "0xD7677B6e62F283E1775B05d9e875B03C27c298a9")
        val value = Value.valueOf(account!!.coinType, Convert.toWei("0.0001", Convert.Unit.ETHER).toBigInteger())
        val gasPrice = FeePerKbFee(Value.valueOf(account!!.coinType, Convert.toWei("20", Convert.Unit.GWEI).toBigInteger()))
        val tx = account!!.createTx(toAddress, value, gasPrice)
        account!!.signTx(tx, AesKeyCipher.defaultKeyCipher())
//        val broadcastResult = account!!.broadcastTx(tx)
//        assertTrue(broadcastResult.errorMessage, broadcastResult.resultType == BroadcastResultType.SUCCESS)
    }

    @Test(expected = GenericInsufficientFundsException::class)
    fun whenTryingToSendMoreThanHaveThenThrow() {
        val coinType = account!!.coinType
        val toAddress = EthAddress(coinType, "0xD7677B6e62F283E1775B05d9e875B03C27c298a9")

        val value = Value.valueOf(coinType, Convert.toWei("1", Convert.Unit.ETHER).toBigInteger())
        val gasPrice = FeePerKbFee(Value.valueOf(coinType, Convert.toWei("20", Convert.Unit.GWEI).toBigInteger()))
        assert(account!!.calculateMaxSpendableAmount(gasPrice.feePerKb.value, null) < value)

        account!!.createTx(toAddress, value, gasPrice)
    }
}
