package fiofoundation.io.fiosdk.models.signatureprovider

import com.google.gson.annotations.SerializedName

class BinaryAbi (accountName: String, abi: String){

    private var account_name: String = ""

    @SerializedName("abi")
    private var _abi: String = ""

    init{
        this.account_name = accountName
        this._abi = abi
    }

    var accountName: String
        get(){return this.account_name}
        set(value){this.account_name = value}

    var abi: String
        get(){return this._abi}
        set(value){this._abi = value}
}