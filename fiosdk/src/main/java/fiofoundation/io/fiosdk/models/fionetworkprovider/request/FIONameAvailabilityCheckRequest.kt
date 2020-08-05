package fiofoundation.io.fiosdk.models.fionetworkprovider.request

class FIONameAvailabilityCheckRequest(fioName: String)  {
    private var fio_name: String = ""

    init{
        this.fio_name = fioName
    }

    var fioName: String
        get(){return this.fio_name}
        set(value){this.fio_name = value}
}