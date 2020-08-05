package fiofoundation.io.fiosdk.models.fionetworkprovider.actions

import com.google.gson.annotations.SerializedName
import fiofoundation.io.fiosdk.models.TokenPublicAddress
import fiofoundation.io.fiosdk.models.fionetworkprovider.Authorization
import fiofoundation.io.fiosdk.models.fionetworkprovider.request.FIORequestData
import java.math.BigInteger

class AddPublicAddressAction(fioAddress: String,
                               tokenPublicAddresses: List<TokenPublicAddress>,
                             maxFee: BigInteger,
                             technologyPartnerId: String,
                               actorPublicKey: String) :
    IAction
{
    override var account = "fio.address"
    override var name = "addaddress"
    override var authorization = ArrayList<Authorization>()
    override var data = ""

    init
    {
        val auth = Authorization(actorPublicKey, "active")
        var requestData =
            FIOAddressRequestData(
                fioAddress,
                tokenPublicAddresses,
                maxFee,
                auth.actor,
                technologyPartnerId
            )

        this.authorization.add(auth)
        this.data = requestData.toJson()
    }

    class FIOAddressRequestData(@field:SerializedName("fio_address") var fioAddress:String,
                                @field:SerializedName("public_addresses") var tokenPublicAddresses:List<TokenPublicAddress>,
                                @field:SerializedName("max_fee") var maxFee:BigInteger,
                                @field:SerializedName("actor") var actor:String,
                                @field:SerializedName("tpid") var technologyPartnerId:String): FIORequestData()
}