package com.mycelium.bequant.remote

import com.mycelium.bequant.remote.model.KYCCreateRequest
import com.mycelium.bequant.remote.model.KYCCreateResponse
import com.mycelium.bequant.remote.model.KYCDocument
import com.mycelium.bequant.remote.model.KYCResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*


interface BequantKYCApiService {
    @POST("eapi/applicant/create")
    suspend fun create(@Body request: KYCCreateRequest): Response<KYCCreateResponse>

    @POST("eapi/applicant/reqmobileverification")
    suspend fun mobileVerification(@Query("uuid") uuid: String): Response<KYCResponse>

    @GET("eapi/applicant/checkmobileverification")
    suspend fun checkMobileVerification(@Query("uuid") uuid: String, @Query("code") code: String): Response<KYCResponse>

    @Multipart
    @POST("eapi/applicant/fileupload?uuid={​uuid}")
    suspend fun uploadFile(@Query("uuid") uuid: String,
                           @Query("id-doc-type") type: KYCDocument,
                           @Query("country-iso-3166-3") country: String,
                           @Part("file") file: MultipartBody.Part): Response<KYCResponse>

    @GET("eapi/applicant/status?uuid=​{uuid}")
    suspend fun status(): KYCResponse
}