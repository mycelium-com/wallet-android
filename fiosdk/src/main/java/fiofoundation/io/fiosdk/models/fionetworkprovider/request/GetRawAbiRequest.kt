package fiofoundation.io.fiosdk.models.fionetworkprovider.request

import com.google.gson.annotations.SerializedName

data class GetRawAbiRequest(@field:SerializedName("account_name") var accountName: String)