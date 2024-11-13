package com.mycelium.giftbox.client.models

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Order status
 * Values: sUCCESS,eRROR,pROCESSING
 */

enum class Status(val value: String) {
    @JsonProperty(value = "pending")
    PENDING("pending"),
    @JsonProperty(value = "payment_confirmed")
    sUCCESS("SUCCESS"),
    @JsonProperty(value = "ERROR")
    eRROR("ERROR"),
    @JsonProperty(value = "PROCESSING")
    pROCESSING("PROCESSING"),
    @JsonProperty(value = "EXPIRED")
    EXPIRED("EXPIRED");
}