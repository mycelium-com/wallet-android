package fiofoundation.io.fiosdk.models

class FIODomain {
    private var fio_domain: String = ""
    private var expiration: String = ""
    private var is_public: Int = 0

    var fioDomain: String
        get(){return this.fio_domain}
        set(value){this.fio_domain = value}
}