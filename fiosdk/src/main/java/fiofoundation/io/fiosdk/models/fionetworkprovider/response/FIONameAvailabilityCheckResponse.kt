package fiofoundation.io.fiosdk.models.fionetworkprovider.response

class FIONameAvailabilityCheckResponse: FIOResponse() {

    private val is_registered: Int = 0

    val isAvailable: Boolean
        get(){return this.is_registered == 0}

}