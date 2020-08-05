package fiofoundation.io.fiosdk.models.fionetworkprovider.actions

import com.google.gson.annotations.SerializedName
import fiofoundation.io.fiosdk.enums.FioDomainVisiblity
import fiofoundation.io.fiosdk.models.fionetworkprovider.Authorization
import fiofoundation.io.fiosdk.models.fionetworkprovider.request.FIORequestData
import java.math.BigInteger

class SetFioDomainVisibilityAction(fioDomain: String,
                             visibility: FioDomainVisiblity,
                             maxFee: BigInteger,
                                   technologyPartnerId: String,
                             actorPublicKey: String) : IAction
{
    override var account = "fio.address"
    override var name = "setdomainpub"
    override var authorization = ArrayList<Authorization>()
    override var data = ""

    init
    {
        val auth = Authorization(actorPublicKey, "active")
        var requestData =
            FioDomainVisibilityData(
                fioDomain,
                visibility.visibility,
                maxFee,
                auth.actor,
                technologyPartnerId
            )

        this.authorization.add(auth)
        this.data = requestData.toJson()
    }

    class FioDomainVisibilityData(@field:SerializedName("fio_domain") var fioDomain:String,
                                @field:SerializedName("is_public") var isPublic:Int,
                                @field:SerializedName("max_fee") var maxFee:BigInteger,
                                @field:SerializedName("actor") var actor:String,
                                @field:SerializedName("tpid") var technologyPartnerId:String): FIORequestData()
}