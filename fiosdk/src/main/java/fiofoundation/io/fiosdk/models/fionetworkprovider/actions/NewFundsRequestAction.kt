package fiofoundation.io.fiosdk.models.fionetworkprovider.actions

import com.google.gson.annotations.SerializedName
import fiofoundation.io.fiosdk.models.fionetworkprovider.Authorization
import fiofoundation.io.fiosdk.models.fionetworkprovider.request.FIORequestData
import java.math.BigInteger

class NewFundsRequestAction(
    payerfioAddress: String,
    payeefioAddress: String,
    content: String,
    maxFee: BigInteger,
    technologyPartnerId: String,
    actorPublicKey: String) : IAction
{
    override var account = "fio.reqobt"
    override var name = "newfundsreq"
    override var authorization = ArrayList<Authorization>()
    override var data = ""

    init
    {
        val auth = Authorization(actorPublicKey, "active")
        var requestData =
            NewFundsRequestData(
                payerfioAddress,
                payeefioAddress,
                content,
                maxFee,
                auth.actor,
                technologyPartnerId
            )

        this.authorization.add(auth)
        this.data = requestData.toJson()
    }

    class NewFundsRequestData(@field:SerializedName("payer_fio_address") var payerFioAddress:String,
                                @field:SerializedName("payee_fio_address") var payeeFioAddress:String,
                                @field:SerializedName("content") var content: String,
                                @field:SerializedName("max_fee") var max_fee:BigInteger,
                                @field:SerializedName("actor") var actor:String,
                                @field:SerializedName("tpid") var technologyPartnerId:String): FIORequestData()
}