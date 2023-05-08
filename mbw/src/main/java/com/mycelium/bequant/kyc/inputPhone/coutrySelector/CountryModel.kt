package com.mycelium.bequant.kyc.inputPhone.coutrySelector

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CountryModel(val name: String, val acronym:String,
                        val acronym3:String,
                        val code: Int,
                        val nationality: String? = null,
                        val description:String? = null,
                        val enabled:Boolean = true) : Parcelable