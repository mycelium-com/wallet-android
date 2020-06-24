package com.mycelium.bequant.remote.client

import retrofit2.Retrofit
import retrofit2.create

inline fun <reified T> createApi(retrofit: Retrofit = RetrofitHolder.retrofit): T = retrofit.create()