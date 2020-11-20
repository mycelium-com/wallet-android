package com.mycelium.bequant.remote

import com.mycelium.bequant.remote.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*


interface BequantKYCApiService {
    @POST("eapi/applicant/create")
    suspend fun create(@Body request: KYCCreateRequest): Response<KYCCreateResponse>

    @PATCH("eapi/applicant/update")
    suspend fun update(@Body request: KYCApplicant): Response<KYCCreateResponse>

    @POST("eapi/applicant/reqmobileverification")
    suspend fun mobileVerification(@Query("uuid") uuid: String): Response<KYCResponse>

    @GET("eapi/applicant/checkmobileverification")
    suspend fun checkMobileVerification(@Query("uuid") uuid: String,
                                        @Query("code") code: String): Response<KYCResponse>

    @POST("eapi/applicant/submit")
    suspend fun submit(@Body onceToken: OnceToken): Response<KYCResponse>

    @Multipart
    @POST("eapi/applicant/fileupload")
    suspend fun uploadFile(@Query("uuid") uuid: String,
                           @Part("id-doc-type") type: RequestBody,
                           @Part("country-iso-3166-3") country: RequestBody,
                           @Part file: MultipartBody.Part): Response<KYCResponse>

    @GET("eapi/applicant/status")
    suspend fun status(@Query("uuid") uuid: String): Response<KYCStatusResponse>

    @GET("eapi/applicant/getuuid")
    suspend fun kycToken(@Query("once-token") onceToken: String): Response<KYCTokenResponse>
}