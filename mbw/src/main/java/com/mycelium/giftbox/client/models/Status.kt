package com.mycelium.giftbox.client.models

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Order status
 * Values: sUCCESS,eRROR,pROCESSING
 */

enum class Status(val value: String) {
    @JsonProperty(value = "pending")
    PENDING("pending"),
    @JsonProperty(value = "card_issued")
    sUCCESS("SUCCESS"),
    @JsonProperty(value = "failed")
    eRROR("ERROR"),
    @JsonProperty(value = "EXPIRED")
    EXPIRED("EXPIRED");
}