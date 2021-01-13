package com.mycelium.wallet.external.partner.model

import com.google.gson.annotations.SerializedName


data class MediaFlowContent(@SerializedName("banner-in-list") val bannersInList: List<MediaFlowBannerInList>,
                            @SerializedName("banner-top") val bannersTop: List<MediaFlowBannerBannerTop>,
                            @SerializedName("banner-bottom-details") val bannersDetails: List<MediaFlowDetailsBannerBottom>)

data class MediaFlowBannerInList(val imageUrl: String,
                                 val link: String,
                                 val index: Int) : CommonContent()

data class MediaFlowBannerBannerTop(val imageUrl: String,
                                    val link: String) : CommonContent()

data class MediaFlowDetailsBannerBottom(val imageUrl: String,
                                        val link: String,
                                        val tag: String) : CommonContent()