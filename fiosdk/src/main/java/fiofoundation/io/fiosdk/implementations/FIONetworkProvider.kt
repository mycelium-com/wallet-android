package fiofoundation.io.fiosdk.implementations

import fiofoundation.io.fiosdk.interfaces.IFIONetworkProvider
import fiofoundation.io.fiosdk.interfaces.IFIONetworkProviderApi

import fiofoundation.io.fiosdk.models.fionetworkprovider.request.*
import fiofoundation.io.fiosdk.models.fionetworkprovider.response.*

import fiofoundation.io.fiosdk.errors.fionetworkprovider.*

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Response

import okhttp3.OkHttpClient
import com.google.gson.Gson
import fiofoundation.io.fiosdk.interfaces.IFIOMockNetworkProviderApi
import fiofoundation.io.fiosdk.models.fionetworkprovider.request.FIONameAvailabilityCheckRequest


class FIONetworkProvider(private val baseURL: String): IFIONetworkProvider
{
    constructor(baseURL: String, mockServerBaseUrl: String): this(baseURL)
    {
        if (mockServerBaseUrl.isNotEmpty())
        {
            val httpClient = OkHttpClient.Builder()

            this.mockRetrofit = Retrofit.Builder()
                .baseUrl(mockServerBaseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build()

            this.mockNetworkProviderApi = this.mockRetrofit!!.create(IFIOMockNetworkProviderApi::class.java)
        }
    }

    private var retrofit: Retrofit
    private var mockRetrofit: Retrofit?

    private var networkProviderApi: IFIONetworkProviderApi
    private var mockNetworkProviderApi: IFIOMockNetworkProviderApi?

    init
    {
        val httpClient = OkHttpClient.Builder()

        this.retrofit = Retrofit.Builder()
            .baseUrl(this.baseURL + "chain/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient.build())
            .build()

        this.mockRetrofit = null

        this.networkProviderApi = this.retrofit.create(IFIONetworkProviderApi::class.java)
        this.mockNetworkProviderApi = null
    }

    private fun <O> processCall(call: Call<O>): O {

        val response: Response<O> = call.execute()
        if (!response.isSuccessful) {
            var responseError: ResponseError? = null
            if (response.errorBody() != null) {
                val gson = Gson()
                responseError = gson.fromJson(response.errorBody()!!.charStream(), ResponseError::class.java)
            }

            if(responseError!=null) {
                responseError.code = response.code()
                throw FIONetworkProviderCallError(responseError)
            }
        }

        @Suppress("UNCHECKED_CAST")
        return response.body() as O
    }

    @Throws(GetPublicAddressError::class)
    override fun getPublicAddress(getPublicAddressRequest: GetPublicAddressRequest): GetPublicAddressResponse{
        try {
            val syncCall = this.networkProviderApi.getPublicAddress(getPublicAddressRequest)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw GetPublicAddressError("",e,e.responseError)
        }
    }

    @Throws(GetFIONamesError::class)
    override fun getFIONames(getFioNamesRequest: GetFIONamesRequest): GetFIONamesResponse{
        try
        {
            val syncCall = this.networkProviderApi.getFIONames(getFioNamesRequest)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw GetFIONamesError("",e,e.responseError)
        }
    }

    @Throws(FIONameAvailabilityCheckError::class)
    override fun isFIONameAvailable(fioNameAvailabilityCheckRequest: FIONameAvailabilityCheckRequest): FIONameAvailabilityCheckResponse{
        try
        {
            val syncCall = this.networkProviderApi.isFIONameAvailable(fioNameAvailabilityCheckRequest)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw FIONameAvailabilityCheckError("",e,e.responseError)
        }
    }

    @Throws(GetFIOBalanceError::class)
    override fun getFIOBalance(getFioBalanceRequest: GetFIOBalanceRequest): GetFIOBalanceResponse{
        try
        {
            val syncCall = this.networkProviderApi.getFIOBalance(getFioBalanceRequest)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw GetFIOBalanceError("",e,e.responseError)
        }
    }

    @Throws(GetFeeError::class)
    override fun getFee(getFeeRequest: GetFeeRequest): GetFeeResponse{
        try
        {
            val syncCall = this.networkProviderApi.getFee(getFeeRequest)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw GetFeeError("",e,e.responseError)
        }
    }

    @Throws(GetInfoError::class)
    override fun getInfo(): GetInfoResponse{
        try
        {
            val syncCall = this.networkProviderApi.getInfo()
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw GetInfoError("",e,e.responseError)
        }
    }

    @Throws(GetBlockError::class)
    override fun getBlock(getBlockRequest: GetBlockRequest): GetBlockResponse{
        try
        {
            val syncCall = this.networkProviderApi.getBlock(getBlockRequest)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw GetFeeError("",e,e.responseError)
        }
    }

    @Throws(GetRawAbiError::class)
    override fun getRawAbi(getRawAbiRequest: GetRawAbiRequest): GetRawAbiResponse{
        try
        {
            val syncCall = this.networkProviderApi.getRawAbi(getRawAbiRequest)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw GetFeeError("",e,e.responseError)
        }
    }

    @Throws(GetPendingFIORequestsError::class)
    override fun getPendingFIORequests(getPendingFioRequests: GetPendingFIORequestsRequest): GetPendingFIORequestsResponse{
        try
        {
            val syncCall = this.networkProviderApi.getPendingFIORequests(getPendingFioRequests)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw GetPendingFIORequestsError("",e,e.responseError)
        }
    }

    @Throws(GetSentFIORequestsError::class)
    override fun getSentFIORequests(getSentFioRequests: GetSentFIORequestsRequest): GetSentFIORequestsResponse{
        try
        {
            val syncCall = this.networkProviderApi.getSentFIORequests(getSentFioRequests)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw GetSentFIORequestsError("",e,e.responseError)
        }
    }

    @Throws(PushTransactionError::class)
    override fun pushTransaction(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    {
        try
        {
            val syncCall = this.networkProviderApi.pushTransaction(pushTransactionRequest)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw PushTransactionError("",e,e.responseError)
        }
    }

    @Throws(GetRequiredKeysError::class)
    override fun getRequiredKeys(getRequiredKeysRequest: GetRequiredKeysRequest): GetRequiredKeysResponse
    {
        try
        {
            val syncCall = this.networkProviderApi.getRequiredKeys(getRequiredKeysRequest)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw GetRequiredKeysError("",e,e.responseError)
        }
    }

    @Throws(PushTransactionError::class)
    override fun registerFioAddress(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    {
        try
        {
            val syncCall = this.networkProviderApi.registerFioAddress(pushTransactionRequest)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw PushTransactionError("",e,e.responseError)
        }
    }

    @Throws(PushTransactionError::class)
    override fun renewFioAddress(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    {
        try
        {
            val syncCall = this.networkProviderApi.renewFioAddress(pushTransactionRequest)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw PushTransactionError("",e,e.responseError)
        }
    }

    @Throws(PushTransactionError::class)
    override fun renewFioDomain(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    {
        try
        {
            val syncCall = this.networkProviderApi.renewFioDomain(pushTransactionRequest)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw PushTransactionError("",e,e.responseError)
        }
    }

    @Throws(PushTransactionError::class)
    override fun registerFioDomain(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    {
        try
        {
            val syncCall = this.networkProviderApi.registerFioDomain(pushTransactionRequest)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw PushTransactionError("",e,e.responseError)
        }
    }

    @Throws(PushTransactionError::class)
    override fun transferTokensToPublicKey(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    {
        try
        {
            val syncCall = this.networkProviderApi.transferTokensToPublicKey(pushTransactionRequest)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw PushTransactionError("",e,e.responseError)
        }
    }

    @Throws(RegisterFIONameForUserError::class)
    override fun registerFioNameOnBehalfOfUser(request: RegisterFIONameForUserRequest): RegisterFIONameForUserResponse
    {
        try
        {
            val syncCall = this.mockNetworkProviderApi!!.registerFioNameForUser(request)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw RegisterFIONameForUserError("",e,e.responseError)
        }
    }

    @Throws(PushTransactionError::class)
    override fun requestNewFunds(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    {
        try
        {
            val syncCall = this.networkProviderApi.requestNewFunds(pushTransactionRequest)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw PushTransactionError("",e,e.responseError)
        }
    }

    @Throws(PushTransactionError::class)
    override fun rejectNewFunds(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    {
        try
        {
            val syncCall = this.networkProviderApi.rejectNewFunds(pushTransactionRequest)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw PushTransactionError("",e,e.responseError)
        }
    }

    @Throws(PushTransactionError::class)
    override fun recordObtData(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    {
        try
        {
            val syncCall = this.networkProviderApi.recordObtData(pushTransactionRequest)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw PushTransactionError("",e,e.responseError)
        }
    }

    @Throws(GetObtDataRequestError::class)
    override fun getObtData(getObtDataRequest: GetObtDataRequest): GetObtDataResponse{
        try
        {
            val syncCall = this.networkProviderApi.getObtData(getObtDataRequest)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw GetObtDataRequestError("",e,e.responseError)
        }
    }

    @Throws(PushTransactionError::class)
    override fun addPublicAddress(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    {
        try
        {
            val syncCall = this.networkProviderApi.addPublicAddress(pushTransactionRequest)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw PushTransactionError("",e,e.responseError)
        }
    }

    @Throws(PushTransactionError::class)
    override fun setFioDomainVisibility(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    {
        try
        {
            val syncCall = this.networkProviderApi.setFioDomainVisibility(pushTransactionRequest)
            return processCall(syncCall)
        }
        catch(e: FIONetworkProviderCallError){
            throw PushTransactionError("",e,e.responseError)
        }
    }


}