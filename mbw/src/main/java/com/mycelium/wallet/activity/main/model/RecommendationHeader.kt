package com.mycelium.wallet.activity.main.model

class RecommendationHeader(val title: String?, val text: String?) :
        RecommendationInfo(HEADER_TYPE) {
    companion object {
        const val HEADER_TYPE = 4
    }
}
