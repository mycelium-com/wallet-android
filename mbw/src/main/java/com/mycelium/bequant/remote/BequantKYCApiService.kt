package com.mycelium.bequant.remote

import com.mycelium.bequant.remote.model.KYCCreateRequest
import com.mycelium.bequant.remote.model.KYCCreateResponse
import com.mycelium.bequant.remote.model.KYCResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


interface BequantKYCApiService {
    @POST("eapi/applicant/create")
    suspend fun create(@Body request: KYCCreateRequest): Response<KYCCreateResponse>

    @POST("eapi/applicant/reqmobileverification")
    suspend fun mobileVerification(@Query("uuid") uuid: String): Response<KYCResponse>

    @GET("eapi/applicant/checkmobileverification")
    suspend fun checkMobileVerification(@Query("uuid") uuid: String, @Query("code") code: String): Response<KYCResponse>

    @POST("eapi/applicant/fileupload?uuid={​uuid}")
    suspend fun uploadFile()

    @GET("eapi/applicant/status?uuid=​{uuid}")
    suspend fun status(): KYCResponse
}