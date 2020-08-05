package fiofoundation.io.fiosdk.models.fionetworkprovider.response

import com.google.gson.annotations.SerializedName

class GetRawAbiResponse(@field:SerializedName("account_name") val accountName:String,
                        @field:SerializedName("abi_hash") val abiHash: String,
                        @field:SerializedName("code_hash") val codeHash:String,
                        @field:SerializedName("abi")  val abi: String): FIOResponse()