package com.mycelium.bequant.remote.repositories

import android.app.Activity
import com.mycelium.bequant.BequantConstants
import com.mycelium.bequant.remote.model.User
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.external.vip.VipRetrofitFactory
import com.mycelium.wallet.external.vip.model.ActivateVipRequest
import com.mycelium.wapi.wallet.AesKeyCipher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECKeyGenerationParameters
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.prng.FixedSecureRandom
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECParameterSpec
import java.security.Security

class UserRepository {
    private val userKeyPair = generateDeterministicECKeyPair()

    private fun generateDeterministicECKeyPair(): AsymmetricCipherKeyPair {
        val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())
        val masterSeed = mbwManager.masterSeedManager.getMasterSeed(AesKeyCipher.defaultKeyCipher())

        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
        val hkdfGenerator = HKDFBytesGenerator(SHA256Digest())
        hkdfGenerator.init(HKDFParameters(masterSeed.bip32Seed, null, ByteArray(0)))
        val privateKeyBytes = ByteArray(32)
        hkdfGenerator.generateBytes(privateKeyBytes, 0, privateKeyBytes.size)

        val ecSpec: ECParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val domainParameters = ECDomainParameters(ecSpec.curve, ecSpec.g, ecSpec.n, ecSpec.h)

        val keyPairGenerator = ECKeyPairGenerator()
        val keyGenParams =
            ECKeyGenerationParameters(domainParameters, FixedSecureRandom(privateKeyBytes))
        keyPairGenerator.init(keyGenParams)
        return keyPairGenerator.generateKeyPair()
    }

    private val _userFlow = MutableStateFlow<User?>(null)
    private val vipApi = VipRetrofitFactory(userKeyPair).createApi()
    val userFlow = _userFlow.filterNotNull()

    // todo mock
    private val preference by lazy {
        WalletApplication.getInstance().getSharedPreferences(
            BequantConstants.PUBLIC_REPOSITORY, Activity.MODE_PRIVATE
        )
    }

    suspend fun identify() {
        val isVIP = preference.getBoolean("VIP", false)
        _userFlow.value = User(status = if (isVIP) User.Status.VIP else User.Status.REGULAR)
    }


    suspend fun applyVIPCode(code: String): User.Status {
        val response = vipApi.activate(ActivateVipRequest(vipCode = code))
        return if (response.done) User.Status.VIP else User.Status.REGULAR
    }
}