package com.mycelium.bequant.kyc.inputPhone.coutrySelector

import com.fasterxml.jackson.annotation.JsonProperty

data class NationalityModel(
        @JsonProperty("num_code") val id: Int,
        @JsonProperty("alpha_2_code") val code2: String,
        @JsonProperty("alpha_3_code") val code3: String,
        @JsonProperty("en_short_name") val country: String,
        @JsonProperty("nationality") val nationality: String
)