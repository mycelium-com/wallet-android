package fiofoundation.io.fiosdk.interfaces

import fiofoundation.io.fiosdk.models.fionetworkprovider.response.*
import fiofoundation.io.fiosdk.models.fionetworkprovider.request.*

import fiofoundation.io.fiosdk.models.fionetworkprovider.request.GetRequiredKeysRequest
import fiofoundation.io.fiosdk.models.fionetworkprovider.response.GetRequiredKeysResponse

interface IFIONetworkProvider {

    fun getPublicAddress(getPublicAddressRequest: GetPublicAddressRequest): GetPublicAddressResponse
    fun getFIONames(getFioNamesRequest: GetFIONamesRequest): GetFIONamesResponse
    fun isFIONameAvailable(fioNameAvailabilityCheckRequest: FIONameAvailabilityCheckRequest): FIONameAvailabilityCheckResponse
    fun getFIOBalance(getFioBalanceRequest: GetFIOBalanceRequest): GetFIOBalanceResponse
    fun getFee(getFeeRequest: GetFeeRequest): GetFeeResponse
    fun getInfo(): GetInfoResponse
    fun getBlock(getBlockRequest: GetBlockRequest): GetBlockResponse
    fun getRawAbi(getRawAbiRequest: GetRawAbiRequest): GetRawAbiResponse
    fun getPendingFIORequests(getPendingFioRequests: GetPendingFIORequestsRequest): GetPendingFIORequestsResponse
    fun getSentFIORequests(getSentFioRequests: GetSentFIORequestsRequest): GetSentFIORequestsResponse
    fun pushTransaction(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    fun getRequiredKeys(getRequiredKeysRequest: GetRequiredKeysRequest): GetRequiredKeysResponse
    fun registerFioAddress(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    fun registerFioDomain(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    fun renewFioDomain(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    fun renewFioAddress(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    fun transferTokensToPublicKey(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    fun registerFioNameOnBehalfOfUser(request: RegisterFIONameForUserRequest): RegisterFIONameForUserResponse
    fun requestNewFunds(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    fun rejectNewFunds(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    fun recordObtData(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    fun getObtData(getObtDataRequest: GetObtDataRequest): GetObtDataResponse
    fun addPublicAddress(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
    fun setFioDomainVisibility(pushTransactionRequest: PushTransactionRequest): PushTransactionResponse
}