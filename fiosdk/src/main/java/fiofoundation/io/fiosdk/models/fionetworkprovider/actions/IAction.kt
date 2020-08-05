package fiofoundation.io.fiosdk.models.fionetworkprovider.actions

import fiofoundation.io.fiosdk.models.fionetworkprovider.Authorization
import java.io.Serializable

interface IAction: Serializable
{
    var account: String
    var name: String
    var authorization: ArrayList<Authorization>
    var data: String
}