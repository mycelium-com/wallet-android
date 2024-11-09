package com.mycelium.giftbox.client.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.parcelize.Parcelize

//"user_id": "123456",
//"order_id": "12345",
//"wallet_address": "1F4ENQ9VHub6utxTsaxRhz7poqVWbqFqdd",
//"wallet_signature": "H/UEFHqmig/vlyXneGNIkxOUOTklkHb1pLfoQo/
//OBlStFAZtpuxMV5ulY0u5022ukrW0ez2KR9ZKzvHArBRs7mw="
@Parcelize
data class MCOrderStatusRequest(
    @JsonProperty("user_id")
    var userId: String,
    @JsonProperty("order_id")
    var orderId: String,
    @JsonProperty("wallet_address")
    var walletAddress: String? = null,
    @JsonProperty("wallet_signature")
    var walletSignature: String? = null
) : Parcelable
