package com.mycelium.wallet.external.changelly

import com.mrd.bitlib.lambdaworks.crypto.Base64
import com.mrd.bitlib.util.HexUtils
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.Security
import java.security.Signature
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.util.UUID
import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException

/**
 * This Interceptor is necessary to comply with changelly's authentication scheme and follows
 * roughly their example implementation in JS:
 * https://github.com/changelly/api-changelly#authentication
 *
 * It wraps the parameters passed in, in a params object and signs the request with the api key secret.
 */
class ChangellyHeaderInterceptor : Interceptor {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response? {
        var request = chain.request()
        val messageBytes: ByteArray = try {
            val params = getParamsFromRequest(request)
            val requestBodyJson = JSONObject()
                .put("id", UUID.randomUUID().toString())
                .put("jsonrpc", "2.0")
                .put("method", getMethodFromRequest(request))
                .put("params", params)
            val dd = requestBodyJson.toString()
                .replace(",", ", ")
                .replace(":", ": ")
            dd.toByteArray()
        } catch (e: JSONException) {
            e.printStackTrace()
            return null
        }
        request = try {
            val privateKey = getPrivateKeys(PRIVATE_KEY)
            val signData = encrypt(messageBytes, privateKey)
            request.newBuilder()
                .delete()
                .url(ExchangeKeys.CHANGELLY_BASE_URL)
                .addHeader(X_API_KEY, PUBLIC_KEY_BASE64)
                .addHeader(X_API_SIGNATURE, Base64.encodeToString(signData, false))
                .addHeader(CONTENT_TYPE, "application/json")
                .post(
                    RequestBody.create(
                        MediaType.parse("application/json; charset=UTF-8"),
                        messageBytes
                    )
                )
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        return chain.proceed(request)
    }

    @Throws(
        InvalidKeySpecException::class,
        NoSuchAlgorithmException::class
    )
    fun getPrivateKeys(privateKey: ByteArray): PrivateKey {
        val spec = PKCS8EncodedKeySpec(privateKey)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePrivate(spec)
    }

    @Throws(
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        InvalidKeyException::class,
        IllegalBlockSizeException::class,
        BadPaddingException::class
    )
    fun encrypt(data: ByteArray?, privateKey: PrivateKey): ByteArray {
        val signer = Signature.getInstance("SHA256withRSA", "BC")
        signer.initSign(privateKey)
        signer.update(data)
        return signer.sign()
    }

    private fun getMethodFromRequest(request: Request): String {
        val pathSegments = request.url().pathSegments()
        return pathSegments[pathSegments.size - 1]
    }

    @Throws(JSONException::class)
    private fun getParamsFromRequest(request: Request): JSONObject {
        val params = JSONObject()
        for (name in request.url().queryParameterNames()) {
            val values = request.url().queryParameterValues(name)
            if (values.size > 1) {
                params.put(name, JSONArray(values))
            } else {
                val value = request.url().queryParameter(name)
                params.put(name, value)
            }
        }
        return params
    }

    companion object {
        const val X_API_KEY = "X-Api-Key"
        const val X_API_SIGNATURE = "X-Api-Signature"
        const val CONTENT_TYPE = "Content-Type"
        val PRIVATE_KEY =
            HexUtils.toBytes(
                "308204bd020100300d06092a864886f70d0101010500048204a7308204a30201000282010100ae0cc822bba7e346b6a4872133723aca554a1cddf833b25352ff5c65138051009b6f30c4429b5cb93f710021c43333c2f92d80b7b6683fa4a438a583561f54dff622d0721aa431eda0e7c8f0c6889874c23bbc1159a72f97298834b417fcdee21ed751954aea26b511768b3f2ed25d32aaa21db92efa1c769fdda4c57d3daf81f73ae6e0febaf6c5f60965342e550cb21a25e7628d1bb9251abb08cb69645006ce3ba1a0574c131a90d815cf552c4de732ba99c53512a84171818cf149ad4073216a0c7ed71ed1c185454fd37b107117d0ed006756a35d0bf8a2964a8d167828f79169ac18fdebd774e1a42301f862fd55fe3898e80e93159852801898435e5d02030100010282010027c515ea11d50ff2b5832cb962670495fd1d1d317f2858e3ff40085db4320ad047b40581a2f29b225d23b5f30140eddfec4e006e7a08a21a3dc80fd1cdd904ecd3f22a8d181752ad48aa0dd92e9441b9d434100a2ae12fd7ab8ecaf427d3091ec40b5141fac73c26fda187b9dd3a063fb08bb09d02e5f5a109707cd8ff801bdd06dd1bd2921114599b03ebae2b89d7aec2db36333e1251fd833a648b64c587a77d3fd07492f32579ffb46fb0d5aa8838d9e53180c284ea404254b3036f644e328a62c6b4ce89e83c52a290fb6cbb4d10f90eac67760d193f06db34e76e99eed88e8287bee2db32865b17b3bddaa995195a8f8fbd318744dd8703ced0b55ec06102818100e44257605bc7336074aa03cdabd272a402a1288faacf15a21391bf8d0b22b6b89182c77d3fe1231d34b0b35d024d9c4ef5d316f93498d2d75d92bd9a1ed98ca3da56d02d54bb4646a13c70434e2127b0451961241340c3816b5f91f543bccd6e0887b4446e37ad2ce4a0e5d5f5b7063a8185e001dac40999e97474e9f756138902818100c333de787328fd8f433901cd81200fbeb1575b56da9d30a6fe1179f7ee44652a40e7bb6912770435f57d6f50899e925af32351f0916a0aa9add2fd7ec0b4641d5bbcde8a9ebccd724289e141796c52fd7c582feb700734375050450fc4db4656cd23014ce2c275d2a87e2b4a49e49bac1dc68d20ae3332071cfd1765777afb35028180737f22092ec74fb6c9b151355b70a3f35b254289d76aeb4e544a963afedc74ca554e70346f03332a4f03ededed016a4b05d5e6b4f8292fde2b89d988477ddaccf9e89d73a28114211eea9eff5f642559eaccd9bb50469fe63777673a4c2917654ac9a7ac4c7cbd928ea8df42f10ec807088cbdf91241c97de883b5b8c11efbc902818050b3e61b58492f2386c04fdd7db01ab255316fa2e5f92cff2d755e3ec1b4673ae3e0aa9bd3357f792b887378119d8c96ab8503c078ee2580674c1edfc39e10f20e56748f4cf773dcf4637acc8dfdda05d0ddc8da06d403a386c1d8fb9f00a50108089be604ae2ef62c1115a6be0c14cc40f730abbd398f4a5f92c7947ca44cad02818100c40418bd0a192030699512dc64ca3031b8a4370d9ac7912661280783e20544f70bbfeceb41649aadd4b64e4755965861a8ef38030bbb0340be24b2e32028f5f999fd95d6a246f0ac89a5d60c4f4f211ad101b4457f6efd026b4b3ca1bff0147726830d99cda9848c8bc974766d0957a76c6237093f2e81f058fc705a4dc1d42a"
            )
        const val PUBLIC_KEY_BASE64 = "bOHMornbqaj//ce3+EKgifLtAugOEg5jPujItaK+YFk="
    }
}
