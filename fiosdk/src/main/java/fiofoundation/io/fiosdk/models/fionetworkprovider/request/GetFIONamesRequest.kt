package fiofoundation.io.fiosdk.models.fionetworkprovider.request

class GetFIONamesRequest (fioPublicKey: String){
    private var fio_public_key: String = fioPublicKey

    var fioPublicKey: String
        get(){return this.fio_public_key}
        set(value){this.fio_public_key = value}
}