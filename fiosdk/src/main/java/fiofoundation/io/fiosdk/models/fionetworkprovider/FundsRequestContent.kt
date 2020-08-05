package fiofoundation.io.fiosdk.models.fionetworkprovider

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import fiofoundation.io.fiosdk.interfaces.ISerializationProvider
import fiofoundation.io.fiosdk.utilities.CryptoUtils
import java.lang.Exception

class FundsRequestContent(
    @field:SerializedName("payee_public_address") var payeeTokenPublicAddress:String,
    @field:SerializedName("amount") var amount:String,
    @field:SerializedName("chain_code") var chainCode: String,
    @field:SerializedName("token_code") var tokenCode: String,
    @field:SerializedName("memo") var memo:String?=null,
    @field:SerializedName("hash") var hash:String?=null,
    @field:SerializedName("offline_url") var offlineUrl:String?=null)
{

    @ExperimentalUnsignedTypes
    fun serialize(privateKey: String, publicKey: String, serializationProvider: ISerializationProvider): String
    {
        val serializedNewFundsContent = serializationProvider.serializeContent(this.toJson(),"new_funds_content")

        val secretKey = CryptoUtils.generateSharedSecret(privateKey,publicKey)

        return CryptoUtils.encryptSharedMessage(serializedNewFundsContent,secretKey,null)
    }

    fun toJson(): String {
        val gson = GsonBuilder().serializeNulls().create()
        return gson.toJson(this,this.javaClass)
    }

    companion object {
        fun deserialize(privateKey: String, publicKey: String, serializationProvider: ISerializationProvider,serializedFundsRequestContent:String):FundsRequestContent?
        {
            try {
                val secretKey = CryptoUtils.generateSharedSecret(privateKey,publicKey)

                val decryptedMessage = CryptoUtils.decryptSharedMessage(serializedFundsRequestContent,secretKey)

                val deserializedMessage = serializationProvider.deserializeContent(decryptedMessage,"new_funds_content")

                return Gson().fromJson(deserializedMessage, FundsRequestContent::class.java)
            }
            catch(e: Exception)
            {
                return null
            }
        }
    }
}