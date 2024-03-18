package com.mycelium.wallet.external.changelly

import com.mrd.bitlib.lambdaworks.crypto.Base64
import com.mrd.bitlib.util.HashUtils
import com.mrd.bitlib.util.HexUtils

import okhttp3.*
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.security.*
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
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
                .url(CHANGELLY_URL)
                .addHeader(X_API_KEY, API_KEY)
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
    fun getPrivateKeys(privateKey: String): PrivateKey {
        val spec = PKCS8EncodedKeySpec(HexUtils.toBytes(privateKey))
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
        const val CHANGELLY_URL = "https://api.changelly.com/v2"
        const val PRIVATE_KEY =
            "308204be020100300d06092a864886f70d0101010500048204a8308204a40201000282010100bc855a39f64f8cfc2f843504e7a09a686b6f97933d408308d2ff78a2a27456090c248b5f35ea00c3a528f2771fa23a5c3db12e9f30c7321da6331fd43d78a76ebb26bc9f4ef8dbaff459f0a12cb1d8011eb745529cb321f0ba92b1ec82d0e5a132d1d7453387ff329c4be0f1ddc1fabd7a656f886a936b50d123c2fcc454d2c689a748f01617662015fea8895b8c00ca42dc1b0e922e54141dc31ef9eef4e32e81d39f526de5412d77d4902a91976904616e7b1efff15b8984fb625108ca8c2ae54621fd7b0c4e8a887d85a132504d34faa3d3060043ddef002db9e1640e1acac6356d9d333b34ed8791e9926a3558d351e37810f5df518c6ed1d6f36ed68007020301000102820100014d96ab11e5c8deb16163906e1d7113c9b252c4e4c67e61603bfdd479f4fde7401b3c8f62eb0428560aeb6a2160d8b06c88bdfec1b28ec91fadf8c959c76cb8da385153749349c97491ee94de9f381401e7586652c8f63218c80ccccab6b0efa54f4802a5718a350a5987eb8411e42ecd1ac863940102dbe3263121d82591f364547b47ded4670ad8099e5370bd7de59f573edaddfbec538c00cd1da1723eb3d467fbe76f6e4a3632cbe56b9177bec81422fdaf223f0f7f12365084cedc1ecb366a07657feaae2ece8aa5d60d431dd9d5a390025d5d6dece9f24b5cffdad427b5600449d6dd1cf21f4bda7b10285351ca29b7e51ca52247e7d592ef4d51471102818100e11a9106f8179743adba0f2671b296441417e889be2cdc3862e07ad0bbe0517cfce9d503a485f0aff7f1773340c3af4e18f9330c4e5b6cc2e572500d4a3ae99af2224091aa0274eccb31fbc4edb05d25d5cd380e1bf1b4b5b838fbb7102d92b089162fb618a55a03c8d20d1c04177f0c56b2429ba14fb6a8c9889ad70bbd329f02818100d665613f84b2756eba83aff53e173829eefb77f712da4ca4ba1f9b70a700240487d8189f68e451e8de357b9991a38c9a12bb753655ed1b5896d735cc4a2fc2960c6f2611385a9b2d6f9dd530fb10553e5658378c4eae1c98dbe67093e1d1d53874a8c1a84a151d76e37eff8c13f453c74610f6d3668184d3c1d6149f5a4b61990281801808731570656c63f0675df8b7c8de5c345cfd19bfb1206df0b890c43a5acfb86d7435a6e6e8d9f29fa12b1dd0bb53bb1dd5754aca0edec4cc24714189fc523695c56c6960e2544377ca455c18186d497dd32439f567cfe85adbd29c0fe11db93559a60c66033962100dc51289a94c8a2fb36683212cd68e9cbdb5f261b1787702818100a47b3cc385638052860757ac37898ace290985fce8dacfa8251ef09ad994730d82c698055c6ca62698abc17a8cd043a344b1ca77f82e2327b0f9c4cd493121010ae30efa71189a2a9e92212825c55f10a71fa0e624cad127b8b52f3355312d7ad58d4e9d74d0843d5cc566faa9a86dc9d90854c4d4c49309fe90e65b66e3a42902818100d69b3969324b22f4f62ebd281d15e05aec884899c6534bfa3604e50505a5f276c74630d9b942cd4cc36be69b8d1e37ad06f24b4034eefdee28533686e58b8df9751380ac0f6d46730b8457fcd5c3a95eb4dc459f2b03b357da9d52fd2ab68726a2001999e630a47f101246e68d5f193e834fe97ae0001973de58e7d8e8285541"
        const val API_KEY = "j1XlBr4+61GC0vyQXjgDw0mCWjei5SODgurlfwYTfzI="
    }
}