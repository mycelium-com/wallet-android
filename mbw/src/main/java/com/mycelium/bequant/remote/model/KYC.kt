package com.mycelium.bequant.remote.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.android.parcel.Parcelize
import java.util.*


@Parcelize
data class KYCRequest(
        var address_1: String? = null,
        var address_2: String? = null,
        var birthday: String? = null,
        var city: String? = null,
        var country: String? = null,
        var first_name: String? = null,
        var last_name: String? = null,
        var nationality: String? = null,
        var zip: String? = null
) : Parcelable


data class KYCCreateRequest(var applicant: KYCApplicant)

data class KYCApplicant(@JsonProperty("phone-full") val phone: String,
                        @JsonInclude(JsonInclude.Include.NON_NULL)
                        var email: String,
                        @JsonInclude(JsonInclude.Include.NON_NULL)
                        @JsonProperty("first_name") var firstName: String? = null,
                        @JsonInclude(JsonInclude.Include.NON_NULL)
                        @JsonProperty("last_name") var lastName: String? = null,
                        @JsonInclude(JsonInclude.Include.NON_NULL)
                        @JsonProperty("dob") var dob: Date? = null,
                        @JsonInclude(JsonInclude.Include.NON_NULL)
                        @JsonProperty("nationality-iso-3166-3") var nationality: String? = null,
                        @JsonInclude(JsonInclude.Include.NON_NULL)
                        @JsonProperty("exchange-user-id") var userId: Int? = null,
                        @JsonInclude(JsonInclude.Include.NON_NULL)
                        @JsonProperty("facta_declaration") var facta: Boolean? = null,
                        @JsonProperty("residential_address") var address: ResidentialAddress = ResidentialAddress())

data class ResidentialAddress(@JsonProperty("address_1") var address1: String = "",
                              @JsonProperty("address_2") var address2: String = "",
                              @JsonProperty("city") var city: String = "",
                              var postcode: String = "",
                              var state: String = "",
                              @JsonProperty("country-iso-3166-3") var country: String = "")


data class KYCCreateResponse(var status: Int?,
                             var message: String?,
                             var uuid: String?,
                             var error: Int?)

data class KYCResponse(var status: Int?,
                       var message: String?,
                       var error: Int?)