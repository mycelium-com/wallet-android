package fiofoundation.io.fiosdk.errors.fionetworkprovider

import fiofoundation.io.fiosdk.models.fionetworkprovider.response.ResponseError

class FIONetworkProviderCallError: FIONetworkProviderError {
    var responseError: ResponseError? = null

    constructor(responseError: ResponseError): super(){
        this.responseError = responseError
    }

    constructor(message: String, responseError: ResponseError): super(message){
        this.responseError = responseError
    }

    constructor(message: String, exception: Exception, responseError: ResponseError): super(message,exception){
        this.responseError = responseError
    }

    constructor(exception: Exception, responseError: ResponseError): super(exception){
        this.responseError = responseError
    }
}