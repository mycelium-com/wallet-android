package fiofoundation.io.fiosdk.models.fionetworkprovider.request

import com.google.gson.annotations.SerializedName

class GetPublicAddressRequest (@field:SerializedName("fio_address") var fioAddress: String,
                               @field:SerializedName("chain_code") var chainCode:String,
                               @field:SerializedName("token_code") var tokenCode:String)