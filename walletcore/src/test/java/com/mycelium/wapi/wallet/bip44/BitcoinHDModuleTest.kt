package com.mycelium.wapi.wallet.bip44

import com.google.common.base.Optional
import com.mrd.bitlib.crypto.*
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.SecureKeyValueStore
import com.mycelium.wapi.wallet.btc.*
import com.mycelium.wapi.wallet.btc.bip44.*
import com.mycelium.wapi.wallet.masterseed.MasterSeedManager
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class BitcoinHDModuleTest {
    private val MASTER_SEED_WORDS = "degree rain vendor coffee push math onion inside pyramid blush stick treat"
    private var bitcoinHDModule: BitcoinHDModule? = null

    @Before
    fun setup() {
        val fakeWapi = mock<Wapi>(Wapi::class.java)

        val backing = InMemoryBtcWalletManagerBacking() as BtcWalletManagerBacking<HDAccountContext>
        val fakeRandomSource = mock<RandomSource>(RandomSource::class.java)
        val store = SecureKeyValueStore(backing, fakeRandomSource)

        val masterSeed = Bip39.generateSeedFromWordList(MASTER_SEED_WORDS.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(), "")
        val masterSeedManager = MasterSeedManager(store)
        masterSeedManager.configureBip32MasterSeed(masterSeed, AesKeyCipher.defaultKeyCipher())

        val fakeMetadataStorage = mock<IMetaDataStorage>(IMetaDataStorage::class.java)
        `when`<Optional<String>>(fakeMetadataStorage.getFirstKeyForCategoryValue(any(String::class.java), any(String::class.java))).thenReturn(Optional.absent())
        `when`<String>(fakeMetadataStorage.getKeyCategoryValueEntry(any(String::class.java), any(String::class.java), any(String::class.java))).thenReturn("")

        bitcoinHDModule = BitcoinHDModule(backing, store, NetworkParameters.productionNetwork,
                fakeWapi, BTCSettings(AddressType.P2SH_P2WPKH, Reference(ChangeAddressMode.PRIVACY)),
                fakeMetadataStorage, null, null, null)
    }

    @Test
    fun whenNoMasterseedDerivedAccountsThenCurrentBip44IndexIsMinusOne() {
        assert(bitcoinHDModule!!.getCurrentBip44Index() == -1)
    }

    @Test
    fun afterAddingFirstMasterseedDerivedAccountCurrentBip44IndexIsZero() {
        bitcoinHDModule!!.createAccount(AdditionalHDAccountConfig())
        assert(bitcoinHDModule!!.getCurrentBip44Index() == 0)
    }

    @Test
    fun afterAddingExternalMasterseedDerivedAccountsCurrentBip44IndexNotAffected() {
        // preparation
        val extSigProvider = mock(ExternalSignatureProvider::class.java)
        `when`(extSigProvider.biP44AccountType).thenReturn(HDAccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER)
        val extSigIndex = 3
        val hdKeyNode = HdKeyNode(PublicKey(HexUtils.toBytes("03af38b37f6cea62da313946a73128b2a4058d7f85de7a7a1aabe31f8eaa66f640")),
                HexUtils.toBytes("a2d926c43e9436ab111588be4bbdf8c375e07c71d2d080220a247d10180a4473"), 3, 0, extSigIndex, BipDerivationType.BIP44)

        // actual test
        val indexBefore = bitcoinHDModule!!.getCurrentBip44Index()
        bitcoinHDModule!!.createAccount(ExternalSignaturesAccountConfig(listOf(hdKeyNode), extSigProvider, extSigIndex))
        val indexAfter = bitcoinHDModule!!.getCurrentBip44Index()
        assert(indexBefore == indexAfter)
    }

    @Test
    fun testTaproot() {
        val backing = InMemoryBtcWalletManagerBacking() as BtcWalletManagerBacking<HDAccountContext>
        val fakeRandomSource = mock<RandomSource>(RandomSource::class.java)
        val store = SecureKeyValueStore(backing, fakeRandomSource)

        val masterSeed = Bip39.generateSeedFromWordList(MASTER_SEED_WORDS.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(), "")
        val masterSeedManager = MasterSeedManager(store)
        masterSeedManager.configureBip32MasterSeed(masterSeed, AesKeyCipher.defaultKeyCipher())
        // Create the base keys for the account
        val root = HdKeyNode.fromSeed(masterSeed.bip32Seed, BipDerivationType.BIP86)
        val keyManager = HDAccountKeyManager.createNew(
            root,
            NetworkParameters.testNetwork,
            7,
            store,
            AesKeyCipher.defaultKeyCipher(),
            BipDerivationType.BIP86
        )

        for (i in 0..19) {
            keyManager?.getAddress(false, i).let {
                System.out.println(/*"path = ${it?.bip32Path}, address = " + */it.toString())
            }
        }
    }
}