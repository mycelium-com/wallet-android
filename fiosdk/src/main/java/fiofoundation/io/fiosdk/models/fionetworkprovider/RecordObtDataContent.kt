package fiofoundation.io.fiosdk.models.fionetworkprovider

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import fiofoundation.io.fiosdk.interfaces.ISerializationProvider
import fiofoundation.io.fiosdk.utilities.CryptoUtils
import java.lang.Exception

/**
 *
 * @param payerTokenPublicAddress Public address on other blockchain of user sending funds.
 * @param payeeTokenPublicAddress Public address on other blockchain of user receiving funds.
 * @param amount Amount sent.
 * @param tokenCode Code of the token represented in Amount requested, i.e. BTC.
 * @param obtId Other Blockchain Transaction ID (OBT ID), i.e Bitcoin transaction ID.
 * @param status Status of this OBT. Allowed statuses are: sent_to_blockchain
 * @param memo
 * @param hash
 * @param offlineUrl
 */
class RecordObtDataContent(
    @field:SerializedName("payer_public_address") var payerTokenPublicAddress:String,
    @field:SerializedName("payee_public_address") var payeeTokenPublicAddress:String,
    @field:SerializedName("amount") var amount:String,
    @field:SerializedName("chain_code") var chainCode: String,
    @field:SerializedName("token_code") var tokenCode: String,
    @field:SerializedName("obt_id") var obtId:String,
    @field:SerializedName("status") var status:String="sent_to_blockchain",
    @field:SerializedName("memo") var memo:String?=null,
    @field:SerializedName("hash") var hash:String?=null,
    @field:SerializedName("offline_url") var offlineUrl:String?=null)
{
    @ExperimentalUnsignedTypes
    fun serialize(privateKey: String, publicKey: String, serializationProvider: ISerializationProvider): String
    {
        val serializedNewFundsContent = serializationProvider.serializeContent(this.toJson(),"record_obt_data_content")

        val secretKey = CryptoUtils.generateSharedSecret(privateKey,publicKey)

        return CryptoUtils.encryptSharedMessage(serializedNewFundsContent,secretKey,null)
    }

    fun toJson(): String {
        val gson = GsonBuilder().serializeNulls().create()
        return gson.toJson(this,this.javaClass)
    }

    companion object {
        fun deserialize(privateKey: String, publicKey: String, serializationProvider: ISerializationProvider,serializedRecordObtDataContent:String):RecordObtDataContent?
        {
            try {
                val secretKey = CryptoUtils.generateSharedSecret(privateKey,publicKey)

                val decryptedMessage = CryptoUtils.decryptSharedMessage(serializedRecordObtDataContent,secretKey)

                val deserializedMessage = serializationProvider.deserializeContent(decryptedMessage,"record_obt_data_content")

                return Gson().fromJson(deserializedMessage, RecordObtDataContent::class.java)
            }
            catch(e: Exception)
            {
                return null
            }
        }
    }
}