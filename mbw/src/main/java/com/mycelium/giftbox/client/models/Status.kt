package com.mycelium.giftbox.client.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

/**
 * Order status
 * Values: sUCCESS,eRROR,pROCESSING
 */


class CustomStatusDeserializer : JsonDeserializer<Status>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext?
    ): Status? =
        when (p.text) {
            "pending", "pending_payment",
            "payment_confirmed", "payment_pending_confirmation",
            "card_requested" ->
                Status.PENDING

            "underpaid", "overpaid" ->
                Status.eRROR

            "card_issued" -> Status.sUCCESS
            "failed" -> Status.eRROR
            "cancelled" -> Status.CANCELLED
            else -> null
        }
}

@JsonDeserialize(using = CustomStatusDeserializer::class)
enum class Status(val value: String) {
    //    @JsonProperty(value = "pending_payment")
    PENDING("pending"),

    //    @JsonProperty(value = "card_issued")
    sUCCESS("SUCCESS"),

    //    @JsonProperty(value = "failed")
    eRROR("ERROR"),

    CANCELLED("CANCELLED"),

    //    @JsonProperty(value = "EXPIRED")
    EXPIRED("EXPIRED");

}

