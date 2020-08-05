package fiofoundation.io.fiosdk.models.fionetworkprovider

import fiofoundation.io.fiosdk.utilities.Utils
import java.io.Serializable

class Authorization(actorPublicAddress: String, var permission: String): Serializable
{
    var actor: String

    init
    {
        actor = Utils.generateActor(actorPublicAddress)
    }
}