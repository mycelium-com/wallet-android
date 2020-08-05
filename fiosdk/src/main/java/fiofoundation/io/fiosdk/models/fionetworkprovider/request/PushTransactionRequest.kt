package fiofoundation.io.fiosdk.models.fionetworkprovider.request

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class PushTransactionRequest(
    @field:SerializedName("signatures") var signatures: List<String>,
    @field:SerializedName("compression") var compression: Int,
    @field:SerializedName("packed_context_free_data") var packagedContextFreeData: String?,
    @field:SerializedName("packed_trx") var packTrx: String):Serializable