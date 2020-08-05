package fiofoundation.io.fiosdk.models

import com.google.gson.annotations.SerializedName

class TokenPublicAddress(
    @field:SerializedName("public_address") var publicAddress: String,
    @field:SerializedName("chain_code") var chainCode: String,
    @field:SerializedName("token_code") var tokenCode: String)