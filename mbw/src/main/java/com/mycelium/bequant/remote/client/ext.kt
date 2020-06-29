package com.mycelium.bequant.remote.client

import com.mycelium.bequant.Constants

inline fun <reified T> createApi(url: String = Constants.ACCOUNT_ENDPOINT_POSTFIX): T = RetrofitHolder.getRetrofit(url).create(T::class.java)