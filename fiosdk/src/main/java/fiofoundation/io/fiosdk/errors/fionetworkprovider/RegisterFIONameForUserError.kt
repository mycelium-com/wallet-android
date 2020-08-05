package fiofoundation.io.fiosdk.errors.fionetworkprovider

import fiofoundation.io.fiosdk.models.fionetworkprovider.response.ResponseError

class RegisterFIONameForUserError: FIONetworkProviderError {
    var responseError: ResponseError? = null

    constructor(message: String, exception: Exception,responseError: ResponseError?): super(message,exception){
        this.responseError = responseError
    }
}