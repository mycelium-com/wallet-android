package com.mycelium.bequant.remote.repositories

import com.mycelium.bequant.remote.model.User
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.external.vip.VipRetrofitFactory
import com.mycelium.wallet.external.vip.model.ActivateVipRequest
import com.mycelium.wallet.update
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

    private val _userFlow = MutableStateFlow<User?>(null)
    private val vipApi = VipRetrofitFactory(userKeyPair).createApi()
    val userFlow = _userFlow.filterNotNull()

    suspend fun identify() {
        val checkResult = vipApi.check()
        // if user is VIP response contains his code else empty string
        val isVIP = checkResult.vipCode.isNotEmpty()
        val status = if (isVIP) User.Status.VIP else User.Status.REGULAR
        _userFlow.update { user -> user?.copy(status = status) ?: User(status) }
    }

    suspend fun applyVIPCode(code: String): User.Status {
        val response = vipApi.activate(ActivateVipRequest(code))
        val status = if (response.done) User.Status.VIP else User.Status.REGULAR
        _userFlow.update { user -> user?.copy(status = status) ?: User(status) }
        return status
    }

    private fun generateDeterministicECKeyPair(): AsymmetricCipherKeyPair {
        val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())
        val masterSeed = mbwManager.masterSeedManager.getMasterSeed(AesKeyCipher.defaultKeyCipher())

        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
        // generate determine asymmetric keys using master seed as random seed
        val hkdfGenerator = HKDFBytesGenerator(SHA256Digest())
        hkdfGenerator.init(HKDFParameters(masterSeed.bip32Seed, null, ByteArray(0)))
        val privateKeyBytes = ByteArray(32)
        hkdfGenerator.generateBytes(privateKeyBytes, 0, privateKeyBytes.size)

        val ecSpec: ECParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val domainParameters = ECDomainParameters(ecSpec.curve, ecSpec.g, ecSpec.n, ecSpec.h)
        val random = FixedSecureRandom(privateKeyBytes)

        val keyGenParams = ECKeyGenerationParameters(domainParameters, random)
        val keyPairGenerator = ECKeyPairGenerator()
        keyPairGenerator.init(keyGenParams)
        return keyPairGenerator.generateKeyPair()
    }

}