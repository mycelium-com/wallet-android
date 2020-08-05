package fiofoundation.io.fiosdk.interfaces

import fiofoundation.io.fiosdk.models.fionetworkprovider.FIOApiEndPoints
import fiofoundation.io.fiosdk.models.fionetworkprovider.request.RegisterFIONameForUserRequest
import fiofoundation.io.fiosdk.models.fionetworkprovider.response.RegisterFIONameForUserResponse

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface IFIOMockNetworkProviderApi {

    @POST(FIOApiEndPoints.register_fio_name_behalf_of_user)
    fun registerFioNameForUser(@Body registerFioNameForUserRequest: RegisterFIONameForUserRequest): Call<RegisterFIONameForUserResponse>

}