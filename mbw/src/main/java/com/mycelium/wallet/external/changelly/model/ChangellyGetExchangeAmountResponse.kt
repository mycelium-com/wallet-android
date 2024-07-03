package com.mycelium.wallet.external.changelly.model

data class ChangellyGetExchangeAmountResponse(
    val from: String,
    val to: String,
    val networkFee: String,
    val amountFrom: String,
    val amountTo: String,
    val max: String,
    val maxFrom: String,
    val maxTo: String,
    val min: String,
    val minFrom: String,
    val minTo: String,
    val visibleAmount: String,
    val rate: String,
    val fee: String,
) {
    val receiveAmount: Double
        get() {
//            val fee = networkFee.toDoubleOrNull() ?: return .0
            val to = amountTo.toDoubleOrNull() ?: return .0
            return to// - fee
        }
}