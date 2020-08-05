package fiofoundation.io.fiosdk.models

class FIOAddress {
    private var fio_address: String = ""
    private var expiration: String = ""

    var fioAddress: String
        get(){return this.fio_address}
        set(value){this.fio_address = value}
}