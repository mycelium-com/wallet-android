package fiofoundation.io.fiosdk.models.fionetworkprovider.response

import com.google.gson.annotations.SerializedName

class RegisterFIONameForUserResponse (
    @field:SerializedName("status") val status:String,
    @field:SerializedName("expiration") val expiration: String): FIOResponse()