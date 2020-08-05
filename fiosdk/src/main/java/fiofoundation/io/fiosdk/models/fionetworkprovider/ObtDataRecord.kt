package fiofoundation.io.fiosdk.models.fionetworkprovider

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import java.math.BigInteger

class ObtDataRecord
{
    @field:SerializedName("fio_request_id") var fioRequestId: BigInteger = BigInteger.ZERO
    @field:SerializedName("payer_fio_address") var payerFioAddress:String = ""
    @field:SerializedName("payee_fio_address") var payeeFioAddress:String = ""
    @field:SerializedName("payer_fio_public_key") var payerFioPublicKey:String = ""
    @field:SerializedName("payee_fio_public_key") var payeeFioPublicKey:String = ""
    @field:SerializedName("content") var content:String = ""
    @field:SerializedName("status") var status:String=""
    @field:SerializedName("time_stamp") var timeStamp:String = ""

    var deserializedContent : RecordObtDataContent? = null

    fun toJson(): String {
        val gson = GsonBuilder().create()
        return gson.toJson(this,this.javaClass)
    }
}