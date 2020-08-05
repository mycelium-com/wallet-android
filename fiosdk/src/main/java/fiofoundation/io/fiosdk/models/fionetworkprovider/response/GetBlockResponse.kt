package fiofoundation.io.fiosdk.models.fionetworkprovider.response

import com.google.gson.annotations.SerializedName
import java.math.BigInteger

class GetBlockResponse: FIOResponse(){

    val id: String? = null
    val producer: String? = null
    val confirmed: BigInteger? = null
    val previous: String? = null
    val transactions: List<String>? = null
    val timestamp: String? = null

    @SerializedName("action_mroot")
    val actionMroot: String? = null

    @SerializedName("block_num")
    val blockNumber: BigInteger? = null

    @SerializedName("ref_block_prefix")
    val refBlockPrefix: BigInteger? = null

    @SerializedName("schedule_version")
    val scheduleVersion: BigInteger? = null

    @SerializedName("transaction_mroot")
    val transactionMRoot: String? = null

    @SerializedName("new_producers")
    val newProducers: String? = null

    @SerializedName("producer_signature")
    val producerSignature: String? = null

    @SerializedName("header_extensions")
    val headerExtensions: List<String>? = null

    @SerializedName("block_extensions")
    val blockExtensions: List<String>? = null
}