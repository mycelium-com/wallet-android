package fiofoundation.io.fiosdk.models.fionetworkprovider.response

import com.google.gson.annotations.SerializedName

class GetPublicAddressResponse: FIOResponse()
{
    @field:SerializedName("public_address") var publicAddress: String = ""
}