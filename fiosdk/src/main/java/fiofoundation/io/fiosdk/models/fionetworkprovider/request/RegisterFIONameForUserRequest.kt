package fiofoundation.io.fiosdk.models.fionetworkprovider.request

import com.google.gson.annotations.SerializedName

class RegisterFIONameForUserRequest (
    @field:SerializedName("fio_name") var fioName: String,
    @field:SerializedName("owner_fio_public_key") var ownerPublickey: String): FIORequest()