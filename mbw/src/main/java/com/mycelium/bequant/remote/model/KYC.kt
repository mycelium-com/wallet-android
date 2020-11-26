package com.mycelium.bequant.remote.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.android.parcel.Parcelize
import java.util.*


@Parcelize
data class KYCRequest(
        var phone: String? = null,
        var address_1: String? = null,
        var address_2: String? = null,
        var birthday: Date? = null,
        var city: String? = null,
        var country: String? = null,
        var first_name: String? = null,
        var last_name: String? = null,
        var nationality: String? = null,
        var zip: String? = null,
        var fatca: Boolean = false,
        val identityList: MutableList<String> = mutableListOf(),
        val poaList: MutableList<String> = mutableListOf(),
        val selfieList: MutableList<String> = mutableListOf(),
        @Transient var isResubmit: Boolean = false
) : Parcelable {
    fun toModel(applicant: KYCApplicant): KYCApplicant {
        applicant.firstName = this.first_name
        applicant.lastName = this.last_name
        applicant.nationality = this.nationality
        applicant.dob = this.birthday
        applicant.address.address1 = this.address_1 ?: ""
        applicant.address.address2 = this.address_2 ?: ""
        applicant.address.city = this.city ?: ""
        applicant.address.country = this.country ?: ""
        applicant.address.postcode = this.zip ?: ""
        applicant.facta = this.fatca
        return applicant
    }
}

data class KYCCreateRequest(var applicant: KYCApplicant)

data class KYCApplicant(@JsonInclude(JsonInclude.Include.NON_NULL) var email: String,
                        @JsonProperty("phone-full") var phone: String? = null,
                        @JsonInclude(JsonInclude.Include.NON_NULL)
                        @JsonProperty("first_name") var firstName: String? = null,
                        @JsonInclude(JsonInclude.Include.NON_NULL)
                        @JsonProperty("last_name") var lastName: String? = null,
                        @JsonInclude(JsonInclude.Include.NON_NULL)
                        @JsonProperty("dob") var dob: Date? = null,
                        @JsonInclude(JsonInclude.Include.NON_NULL)
                        @JsonProperty("nationality-iso-3166-3") var nationality: String? = null,
                        @JsonInclude(JsonInclude.Include.NON_NULL)
                        @JsonProperty("once-token") var userId: String? = null,
                        @JsonInclude(JsonInclude.Include.NON_NULL)
                        @JsonProperty("facta_declaration") var facta: Boolean? = null,
//                        @JsonProperty("account_type") val accountType: String = "pro",
                        @JsonProperty("residential_address") var address: ResidentialAddress = ResidentialAddress())

data class OnceToken(@JsonProperty("once-token") var userId: String)

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

data class KYCStatusResponse(var status: Int?,
                             var message: StatusMessage?,
                             var error: Int?)

data class StatusMessage(val global: KYCStatus,
                         @JsonProperty("global_message") val message: String,
                         val submitted: Boolean? = null,
                         @JsonProperty("submitted_timestamp") val submitDate: Date? = null,
                         val sections: List<Map<String, Boolean>>)

data class KYCTokenResponse(var status: Int?,
                            var message: KYCTokenMessage?,
                            var error: Int?)

data class KYCTokenMessage(var uuid: String)

data class KYCResponse(var status: Int?,
                       var message: String?,
                       var error: Int?)

enum class KYCDocument {
    PASSPORT, ID_CARD_FRONT_SIDE, ID_CARD_BACK_SIDE, FACTA, SELFIE, BANK_STATEMENT_FOR_FIAT,
    EXP, POA, DRIVERS_FRONT_SIDE, DRIVERS_BACK_SIDE
}

enum class KYCStatus {
    NONE, PENDING, INCOMPLETE, REJECTED, VERIFIED, APPROVED,
    @JsonProperty("SIGNED-OFF")
    SIGNED_OFF
}