package com.mycelium.bequant.remote.client

inline fun <reified T> createApi(url: String = RetrofitHolder.BASE_URL + RetrofitHolder.VERSION_POSTFIX): T = RetrofitHolder.getRetrofit(url).create(T::class.java)