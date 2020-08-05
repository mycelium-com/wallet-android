package fiofoundation.io.fiosdk.models.fionetworkprovider.response

import com.google.gson.annotations.SerializedName
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent

class GetPendingFIORequestsResponse: FIOResponse()
{
    @field:SerializedName("requests") var requests: ArrayList<FIORequestContent>
    @field:SerializedName("more") var more: Int=0

    init {
        requests = arrayListOf()
    }
}