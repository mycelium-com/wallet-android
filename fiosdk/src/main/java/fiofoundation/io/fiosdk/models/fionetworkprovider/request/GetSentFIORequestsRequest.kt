package fiofoundation.io.fiosdk.models.fionetworkprovider.request

import com.google.gson.annotations.SerializedName

class GetSentFIORequestsRequest (
    @field:SerializedName("fio_public_key") var requesteeFioPublicKey: String,
    @field:SerializedName("limit") var limit:Int?=null,
    @field:SerializedName("offset") var offset:Int?=null): FIORequest()